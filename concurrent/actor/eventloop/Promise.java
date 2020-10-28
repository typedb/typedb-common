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

package grakn.common.concurrent.actor.eventloop;

import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

@NotThreadSafe
public abstract class Promise<V> {

    @CheckReturnValue
    public static <V> Promise<V> of(V value) {
        return new Precomputed<>(value);
    }

    @CheckReturnValue
    public static <V> Promise<V> compute(final EventLoop eventLoop, final Supplier<V> job) {
        final Computed<V> promise = new Computed<>();
        eventLoop.submit(() -> {
            final V v = job.get();
            promise.complete(v);
        });
        return promise;
    }

    public abstract V await() throws InterruptedException;

    /**
     * Not recommended, this basically just ignores error handling.
     *
     * @return The value from the promise.
     */
    @Deprecated
    public V awaitUnchecked() {
        try {
            return await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static class Precomputed<V> extends Promise<V> {
        private final V value;
        Precomputed(V value) {
            this.value = value;
        }

        @Override
        public V await() {
            return value;
        }

    }

    private static class Computed<V> extends Promise<V> {
        private V value;
        private boolean completed = false;
        private ConcurrentLinkedQueue<Consumer<V>> waiters = new ConcurrentLinkedQueue<>();

        private synchronized void complete(V value) {
            if (completed) return;
            this.value = value;
            this.completed = true;
            for (Consumer<V> waiter : waiters) {
                waiter.accept(value);
            }
            waiters.clear();
            notifyAll();
        }

        @Override
        public synchronized V await() throws InterruptedException {
            while (!completed) {
                wait();
            }
            return value;
        }
    }
}
