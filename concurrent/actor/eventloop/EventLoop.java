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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

// TODO:
//  This class should be optimised, most specifically, it should probably
//  use a separate scheduled timer thread event loop for the whole system to
//  provide efficient and reliable timer events.
public class EventLoop {
    private static final Logger LOG = LoggerFactory.getLogger(EventLoop.class);

    private final TransferQueue<Runnable> jobs = new LinkedTransferQueue<>();
    private final LogicalTimerQueue<Runnable> scheduledJobs = new LogicalTimerQueue<>();
    private final Thread thread;

    private State state;
    private enum State { READY, RUNNING, STOPPED }

    public EventLoop(ThreadFactory factory) {
        thread = factory.newThread(this::loop);
        state = State.READY;
        thread.start();
    }

    public void submit(Runnable job) {
        jobs.offer(job);
    }

    public EventLoop.ScheduledJob submit(long delayMs, Runnable job) {
        return new ScheduledJob(delayMs, job);
    }

    public void await() throws InterruptedException {
        thread.join();
    }

    public void stop() throws InterruptedException {
        submit(() -> state = State.STOPPED);
        thread.join();
    }

    private void loop() {
        LOG.debug("Starting EventLoop");
        state = State.RUNNING;
        try {
            while (state == State.RUNNING) {
                // TODO review performance, might want to batch some events from the regular queue before checking timers
                long currentTime = GlobalSystem.time();
                Runnable scheduledJob = scheduledJobs.poll(currentTime);
                if (scheduledJob != null) {
                    scheduledJob.run();
                } else {
                    Runnable event = jobs.poll(scheduledJobs.timeToNext(currentTime), TimeUnit.MILLISECONDS);
                    if (event != null) {
                        event.run();
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("EventLoop error", e);
        }
        state = State.STOPPED;
        LOG.debug("Stopped EventLoop");
    }

    public class ScheduledJob {
        private LogicalTimerQueue<Runnable>.LogicalTimedItem job;

        ScheduledJob(long deadlineMs, Runnable job) {
            submit(() -> this.job = scheduledJobs.offer(deadlineMs, job));
        }

        public void cancel() {
            submit(() -> job.cancel());
        }
    }
}
