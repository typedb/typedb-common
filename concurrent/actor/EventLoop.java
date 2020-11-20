/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.common.concurrent.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EventLoop {
    private static final Logger LOG = LoggerFactory.getLogger(EventLoop.class);
    private enum State { READY, RUNNING, STOPPED }

    private State state;
    private final TransferQueue<Job> jobs = new LinkedTransferQueue<>();
    private final ScheduledJobQueue scheduledJobs = new ScheduledJobQueue();
    private final Thread thread;

    public EventLoop(ThreadFactory factory) {
        thread = factory.newThread(this::loop);
        state = State.READY;
        thread.start();
    }

    public void submit(Runnable job, Consumer<Exception> errorHandler) {
        jobs.offer(new Job(job, errorHandler));
    }

    public EventLoop.Cancellable submit(long scheduleMs, Runnable job, Consumer<Exception> errorHandler) {
        final AtomicReference<Cancellable> cancellable = new AtomicReference<>();
        submit(() -> cancellable.set(scheduledJobs.offer(scheduleMs, new Job(job, errorHandler))), errorHandler);
        return cancellable.get();
    }

    public void await() throws InterruptedException {
        thread.join();
    }

    public void stop() throws InterruptedException {
        submit(
                () -> state = State.STOPPED,
                e -> LOG.error("An unexpected error has occurred.", e)
        );
        await();
    }

    private void loop() {
        LOG.debug("Started");
        state = State.RUNNING;

        while (state == State.RUNNING) {
            long currentTime = Clock.time();
            Job scheduledJob = scheduledJobs.poll(currentTime);
            if (scheduledJob != null) {
                scheduledJob.run();
            } else {
                try {
                    Job job = jobs.poll(scheduledJobs.timeToNext(currentTime), TimeUnit.MILLISECONDS);
                    if (job != null) {
                        job.run();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        state = State.STOPPED;
        LOG.debug("stopped");
    }

    public static class Clock {
        private static Supplier<Long> getTime = () -> System.currentTimeMillis();
        private static Random random = ThreadLocalRandom.current();

        private Clock() {}

        public static void set(Supplier<Long> getTime, Random random) {
            Clock.getTime = getTime;
            Clock.random = random;
        }

        public static long time() {
            return getTime.get();
        }

        public static Random random() {
            return random;
        }
    }
    
    public static class Cancellable implements Comparable<Cancellable> {
        private final long version;
        private final long expireAtMs;
        private final Job job;
        private boolean cancelled = false;

        public Cancellable(long version, long expireAtMs, Job job) {
            this.expireAtMs = expireAtMs;
            this.job = job;
            this.version = version;
        }

        @Override
        public int compareTo(Cancellable other) {
            if (expireAtMs < other.expireAtMs) {
                return -1;
            } else if (expireAtMs > other.expireAtMs) {
                return 1;
            } else {
                return Long.compare(version, other.version);
            }
        }

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    private static class Job {
        private final Runnable job;
        private final Consumer<Exception> errorHandler;

        public Job(Runnable job, Consumer<Exception> errorHandler) {
            this.job = job;
            this.errorHandler = errorHandler;
        }

        public void run() {
            try {
                job.run();
            } catch (Exception e) {
                errorHandler.accept(e);
            }
        }
    }

    private static class ScheduledJobQueue {
        private final PriorityQueue<Cancellable> queue = new PriorityQueue<>();
        private long counter = 0L;

        public Cancellable offer(long expireAtMs, Job job) {
            counter++;
            Cancellable item = new Cancellable(counter, expireAtMs, job);
            queue.add(item);
            return item;
        }

        public Job poll(long currentTimeMs) {
            Cancellable timer = peekToNextReady();
            if (timer == null) return null;
            if (timer.expireAtMs > currentTimeMs) return null;
            queue.poll();
            return timer.job;
        }

        public long timeToNext(long currentTimeMs) {
            Cancellable timer = peekToNextReady();
            if (timer == null) return Long.MAX_VALUE;
            return timer.expireAtMs - currentTimeMs;
        }

        private Cancellable peekToNextReady() {
            Cancellable item;
            while ((item = queue.peek()) != null && item.isCancelled()) {
                queue.poll();
            }
            return item;
        }
    }
}