package grakn.cluster.execution;

import grakn.cluster.execution.eventloop.EventLoop;
import grakn.cluster.execution.eventloop.GlobalSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ActorPeriodicTask<STATE extends Actor.State<STATE>> {
    private static final Logger LOG = LoggerFactory.getLogger(ActorPeriodicTask.class);

    private final Actor<STATE> actor;
    private final int timeoutMs;
    private final int variationMs;
    private final Consumer<STATE> onTimeout;
    private EventLoop.ScheduledJob timer;

    private long version = 0;

    public ActorPeriodicTask(Actor<STATE> actor, int timeoutMs, int variationMs, Consumer<STATE> onTimeout) {
        this.actor = actor;
        this.timeoutMs = timeoutMs;
        this.variationMs = variationMs;
        this.onTimeout = onTimeout;
        timer = null;
    }

    public void restart() {
        stop();
        start();
    }

    public void once() {
        stop();
        long expireAt = calculateExpireAtMillis();
        final long taskVersion = ++version;
        timer = actor.schedule(
                expireAt,
                state -> {
                    if (taskVersion != version) return;
                    onTimeout.accept(state);
                });
    }

    public void stop() {
        if (timer == null) return;
        version++;
        timer.cancel();
    }

    private long calculateExpireAtMillis() {
        long variationMs = this.variationMs > 0 ? GlobalSystem.random().nextInt(this.variationMs) + 1 : 0;
        long currentMillis = GlobalSystem.time();
        return currentMillis + timeoutMs + variationMs;
    }

    private void start() {
        long expireAt = calculateExpireAtMillis();
        final long taskVersion = ++version;
        timer = actor.schedule(
                expireAt,
                state -> {
                    if (taskVersion != version) return;
                    onTimeout.accept(state);
                    start(); // TODO find a better way to do this
                });
    }
}
