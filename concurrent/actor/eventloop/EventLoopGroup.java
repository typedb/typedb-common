package grakn.common.concurrent.actor.eventloop;

import grakn.common.concurrent.NamedThreadFactory;

public class EventLoopGroup {
    private final EventLoop[] eventLoops;
    private int nextIndex;

    public EventLoopGroup(int threadCount, String prefix) {
        NamedThreadFactory threadFactory = new NamedThreadFactory(prefix);
        eventLoops = new EventLoop[threadCount];
        for (int i = 0; i < threadCount; i++) {
            eventLoops[i] = new EventLoop(threadFactory);
        }
        nextIndex = 0;
    }

    public synchronized EventLoop assignEventLoop() {
        EventLoop eventLoop = eventLoops[nextIndex];
        nextIndex = (nextIndex + 1) % eventLoops.length;
        return eventLoop;
    }

    public synchronized void stop() throws InterruptedException {
        for (int i = 0; i < eventLoops.length; i++) {
            eventLoops[i].stop();
        }
    }
}
