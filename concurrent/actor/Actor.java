/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.common.concurrent.actor;

import grakn.common.concurrent.actor.eventloop.EventLoop;
import grakn.common.concurrent.actor.eventloop.EventLoopGroup;

import javax.annotation.CheckReturnValue;
import java.util.concurrent.CompletableFuture;
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

        protected <CHILD_STATE extends State<CHILD_STATE>> Actor<CHILD_STATE> child(Function<Actor<CHILD_STATE>, CHILD_STATE> stateConstructor) {
            return Actor.root(self.eventLoopGroup, stateConstructor);
        }

        protected Actor<STATE> self() {
            assert this.self != null : ERROR_SELF_ACTOR_IS_NULL;
            return this.self;
        }
    }

    public STATE state;
    private final EventLoopGroup eventLoopGroup;
    private final EventLoop eventLoop;

    private Actor(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        this.eventLoop = eventLoopGroup.assignEventLoop();
    }

    public static <ROOT_STATE extends State<ROOT_STATE>> Actor<ROOT_STATE> root(EventLoopGroup eventLoopGroup, Function<Actor<ROOT_STATE>, ROOT_STATE> stateConstructor) {
        Actor<ROOT_STATE> actor = new Actor<>(eventLoopGroup);
        actor.state = stateConstructor.apply(actor);
        return actor;
    }

    public void tell(Consumer<STATE> job) {
        assert state != null : ERROR_STATE_IS_NULL;
        eventLoop.submit(() -> job.accept(state));
    }

    @CheckReturnValue
    public CompletableFuture<Void> order(Consumer<STATE> job) {
        return ask(state -> {
            job.accept(state);
            return null;
        });
    }

    @CheckReturnValue
    public <ANSWER> CompletableFuture<ANSWER> ask(Function<STATE, ANSWER> job) {
        assert state != null : ERROR_STATE_IS_NULL;
        CompletableFuture<ANSWER> future = new CompletableFuture<>();
        eventLoop.submit(() -> {
            try {
                future.complete(job.apply(state));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public EventLoop.ScheduledJob schedule(long millis, Consumer<STATE> job) {
        assert state != null : ERROR_STATE_IS_NULL;
        return eventLoop.submit(millis, () -> job.accept(state));
    }
}
