package grakn.cluster.execution;

import grakn.cluster.execution.eventloop.EventLoopSingleThreaded;
import grakn.cluster.execution.eventloop.Promise;
import grakn.cluster.execution.testkit.TestState;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ActorTest {
    private BlockingQueue<String> results;

    @Before
    public void setup() {
        results = new LinkedBlockingQueue<>();
    }

    @Test
    public void periodicTask_mustNotBeInvokedIfItIsStoppedBeforeTheTimeoutHasElapsed() throws InterruptedException {
        EventLoopSingleThreaded rootLoop = new EventLoopSingleThreaded(namedThreadFactory("rootLoop"));
        Actor<TestState> root = Actor.root(rootLoop, actor -> new TestState(actor));
        AtomicInteger count = new AtomicInteger();
        ActorPeriodicTask<TestState> periodicTask = new ActorPeriodicTask<>(root, 1000, 0, msg -> {
            count.incrementAndGet();
        });
        periodicTask.once();
        periodicTask.stop();
        Thread.sleep(1500);
        assertEquals(0, count.get());
    }

    @Test
    public void testActors() throws InterruptedException {
        EventLoopSingleThreaded rootLoop = new EventLoopSingleThreaded(namedThreadFactory("rootLoop"));
        EventLoopSingleThreaded sessionLoop = new EventLoopSingleThreaded(namedThreadFactory("sessionLoop"));
        EventLoopSingleThreaded txLoop = new EventLoopSingleThreaded(namedThreadFactory("txLoop"));


        Promise<Object> test = Promise.compute(rootLoop, () -> {
            Actor<TestState> root = Actor.root(rootLoop, actor -> new TestState(actor));
            root.ask(state -> state.<Session>child(sessionActor -> new Session(sessionActor, sessionLoop))).then(sessionActor -> {
                Promise<Actor<Transaction>> txActorPromise = sessionActor.askAsync(sess -> sess.openTransaction("tx"));
                txActorPromise.thenDefer(tx -> tx.ask(Transaction::getName).thenDefer(results::add));
                return txActorPromise;
            }).thenDefer(txActorPromise -> {
                txActorPromise.thenDefer(tx -> {
                    tx.ask(Transaction::getName).thenDefer(results::add);
                });
            });
            return null;
        });

        sleep(100);

        rootLoop.stop();
        sessionLoop.stop();
        txLoop.stop();

        assertEquals(2, results.size());
    }

    private static class Session extends Actor.State<Session> {
        private final EventLoopSingleThreaded txLoop;
        private final List<Actor<Transaction>> transactions = new ArrayList<>();

        private Session(Actor<Session> self, EventLoopSingleThreaded txLoop) {
            super(self);
            this.txLoop = txLoop;
        }

        public Promise<Actor<Transaction>> openTransaction(String name) {
            Actor<Transaction> txActor = this.child(actor -> new Transaction(actor, name));
            transactions.add(txActor);
            return Promise.of(txActor);
        }
    }

    private static class Transaction extends Actor.State<Transaction> {
        private final String name;

        private Transaction(Actor<Transaction> self, String name) {
            super(self);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static ThreadFactory namedThreadFactory(String name) {
        return r -> namedThread(name, r);
    }

    private static Thread namedThread(String name, Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(name);
        return thread;
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
