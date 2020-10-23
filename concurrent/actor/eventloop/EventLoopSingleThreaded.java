package grakn.cluster.execution.eventloop;

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
public class EventLoopSingleThreaded implements EventLoop {
    private static final Logger LOG = LoggerFactory.getLogger(EventLoopSingleThreaded.class);

    private final TransferQueue<Runnable> eventQueue = new LinkedTransferQueue<>();
    private final LogicalTimerQueue<Runnable> timerQueue = new LogicalTimerQueue<>();
    private final Thread thread;

    private State state;
    private enum State { READY, RUNNING, STOPPED }

    public EventLoopSingleThreaded(ThreadFactory factory) {
        thread = factory.newThread(this::loop);
        state = State.READY;
        thread.start();
    }

    @Override
    public void submit(Runnable job) {
        eventQueue.offer(job);
    }

    @Override
    public EventLoop.ScheduledJob submit(long millis, Runnable job) {
        return new ScheduledJob(millis, job);
    }

    public void stop() throws InterruptedException {
        submit(() -> state = State.STOPPED);
        thread.join();
    }

    private void loop() {
        LOG.debug("Starting EventLoop");
        state = State.RUNNING;
        try {
            EventLoopThreadChecker.setThreadEventLoop(this);
            while (state == State.RUNNING) {
                // TODO review performance, might want to batch some events from the regular queue before checking timers
                long currentTime = GlobalSystem.time();
                Runnable runnable = timerQueue.poll(currentTime);
                if (runnable != null) {
                    runnable.run();
                } else {
                    Runnable event = eventQueue.poll(timerQueue.timeToNext(currentTime), TimeUnit.MILLISECONDS);
                    if (event != null) {
                        event.run();
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error("EventLoop error", ex);
        }
        state = State.STOPPED;
        LOG.debug("Stopped EventLoop");
    }

    private class ScheduledJob implements EventLoop.ScheduledJob {
        private LogicalTimerQueue<Runnable>.LogicalTimedItem timer;

        ScheduledJob(long millis, Runnable runnable) {
            submit(() -> timer = timerQueue.offer(millis, runnable));
        }

        @Override
        public void cancel() {
            submit(() -> timer.cancel());
        }
    }
}
