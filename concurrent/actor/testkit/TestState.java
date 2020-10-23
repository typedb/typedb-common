package grakn.cluster.execution.testkit;

import grakn.cluster.execution.Actor;

import java.util.function.Function;

public class TestState extends Actor.State<TestState> {
    public TestState(Actor<TestState> self) {
        super(self);
    }

    public <S extends Actor.State<S>> Actor<S> createChild(Function<Actor<S>, S> stateConstructor) {
        return this.child(stateConstructor);
    }
}
