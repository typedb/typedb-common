package grakn.cluster.execution;

import grakn.cluster.execution.eventloop.EventLoop;
import grakn.cluster.execution.eventloop.Promise;

import javax.annotation.CheckReturnValue;
import java.util.function.Consumer;
import java.util.function.Function;

public class Actor<STATE extends Actor.State<STATE>> {
    private static String ERROR_SELF_ACTOR_IS_NULL = "The self actor should always be non-null.";
    private static String ERROR_STATE_IS_NULL = "Cannot process actor message when the state hasn't been setup. Are you calling the method from state constructor?";

    public static abstract class State<STATE extends State<STATE>> {
        private Actor<STATE> self;

        protected State(Actor<STATE> self) {
            this.self = self;
        }

        protected <CHILD_STATE extends State<CHILD_STATE>> Actor<CHILD_STATE> child(EventLoop eventLoop, Function<Actor<CHILD_STATE>, CHILD_STATE> stateConstructor) {
            Actor<CHILD_STATE> actor = new Actor<>(eventLoop);
            actor.state = stateConstructor.apply(actor);
            return actor;
        }

        protected <CHILD_STATE extends State<CHILD_STATE>> Actor<CHILD_STATE> child(Function<Actor<CHILD_STATE>, CHILD_STATE> stateConstructor) {
            return child(self.eventLoop, stateConstructor);
        }

        protected Actor<STATE> self() {
            assert this.self != null : ERROR_SELF_ACTOR_IS_NULL;
            return this.self;
        }
    }

    private STATE state;
    private final EventLoop eventLoop;

    private Actor(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public static <ROOT_STATE extends State<ROOT_STATE>> Actor<ROOT_STATE> root(EventLoop eventLoop, Function<Actor<ROOT_STATE>, ROOT_STATE> stateConstructor) {
        Actor<ROOT_STATE> actor = new Actor<>(eventLoop);
        actor.state = stateConstructor.apply(actor);
        return actor;
    }

    public void tell(Consumer<STATE> job) {
        assert state != null : ERROR_STATE_IS_NULL;
        // assign it to a variable to suppress @CheckReturnValue deliberately
        Promise<Object> compute = Promise.compute(eventLoop, () -> {
            job.accept(state);
            return null;
        });
    }

    @CheckReturnValue
    public Promise<Void> order(Consumer<STATE> job) {
        assert state != null : ERROR_STATE_IS_NULL;
        return Promise.compute(eventLoop, () -> {
            job.accept(state);
            return null;
        });
    }

    @CheckReturnValue
    public <ANSWER> Promise<ANSWER> ask(Function<STATE, ANSWER> job) {
        assert state != null : ERROR_STATE_IS_NULL;
        return Promise.compute(eventLoop, () -> job.apply(state));
    }

    @CheckReturnValue
    public <ANSWER> Promise<ANSWER> askAsync(Function<STATE, Promise<ANSWER>> jobAsync) {
        assert state != null : ERROR_STATE_IS_NULL;
        return Promise.computeAsync(eventLoop, () -> jobAsync.apply(state));
    }

    public EventLoop.ScheduledJob schedule(long millis, Consumer<STATE> job) {
        assert state != null : ERROR_STATE_IS_NULL;
        return eventLoop.submit(millis, () -> job.accept(state));
    }
}
