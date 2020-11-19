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

package grakn.common.concurrent.actor.eventloop;

import grakn.common.collection.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.function.Consumer;

import static grakn.common.collection.Collections.pair;

// TODO:
//  This class should be optimised, most specifically, it should probably
//  use a separate scheduled timer thread event loop for the whole system to
//  provide efficient and reliable timer events.
public class EventLoop {
    private static final Logger LOG = LoggerFactory.getLogger(EventLoop.class);

    private final TransferQueue<Pair<Runnable, Consumer<Exception>>> jobs = new LinkedTransferQueue<>();
    private final LogicalTimerQueue<Pair<Runnable, Consumer<Exception>>> scheduledJobs = new LogicalTimerQueue<>();
    private final Thread thread;
    private final Consumer<Exception> errorHandler = e -> { LOG.error("An unexpected error has occurred.", e); };

    private State state;
    private enum State { READY, RUNNING, STOPPED }

    public EventLoop(ThreadFactory factory) {
        thread = factory.newThread(this::loop);
        state = State.READY;
        thread.start();
    }

    public void submit(Runnable job, Consumer<Exception> onError) {
        jobs.offer(pair(job, onError));
    }

    public EventLoop.ScheduledJob submit(long scheduleMs, Runnable job, Consumer<Exception> errorHandler) {
        return new ScheduledJob(scheduleMs, job, errorHandler);
    }

    public void await() throws InterruptedException {
        thread.join();
    }

    public void stop() throws InterruptedException {
        submit(() -> state = State.STOPPED, errorHandler);
        thread.join();
    }

    private void loop() {
        LOG.debug("Started");
        state = State.RUNNING;

        while (state == State.RUNNING) {
            long currentTime = GlobalSystem.time();
            Pair<Runnable, Consumer<Exception>> scheduledJob = scheduledJobs.poll(currentTime);
            if (scheduledJob != null) {
                try {
                    scheduledJob.first().run();
                } catch (Exception e) {
                    scheduledJob.second().accept(e);
                }
            } else {
                try {
                    Pair<Runnable, Consumer<Exception>> job = jobs.poll(scheduledJobs.timeToNext(currentTime), TimeUnit.MILLISECONDS);
                    if (job != null) {
                        try {
                            job.first().run();
                        } catch (Exception e) {
                            job.second().accept(e);
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        state = State.STOPPED;
        LOG.debug("stopped");
    }

    public class ScheduledJob {
        private LogicalTimerQueue<Pair<Runnable, Consumer<Exception>>>.LogicalTimedItem timer;

        ScheduledJob(long scheduleMs, Runnable job, Consumer<Exception> errorHandler) {
            submit(() -> timer = scheduledJobs.offer(scheduleMs, pair(job, errorHandler)), errorHandler);
        }

        public void cancel() {
            submit(() -> timer.cancel(), errorHandler);
        }
    }
}