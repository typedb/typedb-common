package grakn.cluster.execution;

import grakn.cluster.execution.eventloop.EventLoopSingleThreaded;
import grakn.cluster.execution.eventloop.Promise;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import static org.junit.Assert.assertEquals;

public class EventLoopTest {

    private BlockingQueue<String> results;

    @Before
    public void setup() {
        results = new LinkedBlockingQueue<>();
    }

    @Test
    public void testPromises() throws Exception {
        EventLoopSingleThreaded main = new EventLoopSingleThreaded(namedThreadFactory("main"));
        EventLoopSingleThreaded inner = new EventLoopSingleThreaded(namedThreadFactory("inner"));

        Promise.compute(main, () -> {
            Promise<String> promise = Promise.compute(inner, () -> "Hello");
            sleep(100);
            promise.thenDefer(v -> results.add(v));
            return null;
        }).await();

        sleep(200);

        main.stop();
        inner.stop();

        assertEquals(1, results.size());
    }

    private static void log(String name, String message) {
        log(name + ": " + message);
    }

    private static void log(String message) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + message);
    }

    private static Thread namedThread(String name, Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(name);
        return thread;
    }

    private static ThreadFactory namedThreadFactory(String name) {
        return r -> namedThread(name, r);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
