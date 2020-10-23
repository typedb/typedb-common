package grakn.cluster.execution.eventloop;

public interface EventLoop {
    void submit(Runnable job);
    ScheduledJob submit(long millis, Runnable job);

    interface ScheduledJob {
        void cancel();
    }
}
