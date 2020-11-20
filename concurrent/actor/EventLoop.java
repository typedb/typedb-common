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

import grakn.common.collection.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PriorityQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static grakn.common.collection.Collections.pair;

public class EventLoop {
    private static final Logger LOG = LoggerFactory.getLogger(EventLoop.class);
    private enum State { READY, RUNNING, STOPPED }

    private final Consumer<Exception> errorHandler = e -> { LOG.error("An unexpected error has occurred.", e); };
    private final TransferQueue<Pair<Runnable, Consumer<Exception>>> jobs = new LinkedTransferQueue<>();
    private final ScheduledJobs scheduledJobs = new ScheduledJobs();
    private final Thread thread;
    private State state;

    public EventLoop(ThreadFactory factory) {
        thread = factory.newThread(this::loop);
        state = State.READY;
        thread.start();
    }

    public void submit(Runnable job, Consumer<Exception> onError) {
        jobs.offer(pair(job, onError));
    }

    public ScheduledJobs.Cancellable submit(long scheduleMs, Runnable job, Consumer<Exception> errorHandler) {
        final AtomicReference<ScheduledJobs.Cancellable> scheduledJob = new AtomicReference<>();
        submit(() -> scheduledJob.set(scheduledJobs.offer(scheduleMs, pair(job, errorHandler))), errorHandler);
        return scheduledJob.get();
    }

    public void await() throws InterruptedException {
        thread.join();
    }

    public void stop() throws InterruptedException {
        submit(() -> state = State.STOPPED, errorHandler);
        await();
    }

    private void loop() {
        LOG.debug("Started");
        state = State.RUNNING;

        while (state == State.RUNNING) {
            long currentTime = EventLoopClock.time();
            Pair<Runnable, Consumer<Exception>> scheduledJob = scheduledJobs.poll(currentTime);
            if (scheduledJob != null) {
                run(scheduledJob);
            } else {
                try {
                    Pair<Runnable, Consumer<Exception>> job = jobs.poll(scheduledJobs.timeToNext(currentTime), TimeUnit.MILLISECONDS);
                    if (job != null) {
                        run(job);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        state = State.STOPPED;
        LOG.debug("stopped");
    }

    private void run(Pair<Runnable, Consumer<Exception>> job) {
        try {
            job.first().run();
        } catch (Exception e) {
            job.second().accept(e);
        }
    }

    public static class ScheduledJobs {
        private final PriorityQueue<Cancellable> queue = new PriorityQueue<>();
        private long counter;

        public Cancellable offer(long expireAtMs, Pair<Runnable, Consumer<Exception>> job) {
            Cancellable item = new Cancellable(expireAtMs, job);
            queue.add(item);
            return item;
        }

        public Pair<Runnable, Consumer<Exception>> poll(long currentTimeMs) {
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

        public class Cancellable implements Comparable<Cancellable> {
            private final long version;
            private final long expireAtMs;
            private final Pair<Runnable, Consumer<Exception>> job;
            private boolean cancelled = false;

            public Cancellable(long expireAtMs, Pair<Runnable, Consumer<Exception>> job) {
                this.expireAtMs = expireAtMs;
                this.job = job;
                version = counter++;
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
    }
}