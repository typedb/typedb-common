package grakn.cluster.execution;

import java.util.function.Function;

public class ActorRoot extends Actor.State<ActorRoot> {
    public ActorRoot(Actor<ActorRoot> self) {
        super(self);
    }

    public <S extends Actor.State<S>> Actor<S> createActor(Function<Actor<S>, S> stateConstructor) {
        return this.child(stateConstructor);
    }
}
