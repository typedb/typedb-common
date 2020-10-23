package grakn.cluster.execution.eventloop;

class EventLoopThreadChecker {
    static final ThreadLocal<EventLoopSingleThreaded> threadEventLoop = ThreadLocal.withInitial(() -> null);

    static void setThreadEventLoop(EventLoopSingleThreaded eventLoop) {
        threadEventLoop.set(eventLoop);
    }

    static boolean isOnThread(EventLoop eventLoop) {
        return threadEventLoop.get() == eventLoop;
    }
}
