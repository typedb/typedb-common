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
import grakn.common.concurrent.actor.eventloop.GlobalSystem;

import java.util.function.Consumer;

public class ActorPeriodicTask<STATE extends Actor.State<STATE>> {
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

    public void schedule() {
        schedule(true);
    }

    public void reschedule() {
        stop();
        schedule(true);
    }

    public void scheduleOnce() {
        schedule(false);
    }

    public void rescheduleOnce() {
        stop();
        schedule(false);
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

    private void schedule(boolean repeat) {
        long expireAt = calculateExpireAtMillis();
        final long taskVersion = ++version;
        timer = actor.schedule(
                expireAt,
                state -> {
                    if (taskVersion != version) return;
                    onTimeout.accept(state);
                    if (repeat) schedule(true);
                });
    }
}
