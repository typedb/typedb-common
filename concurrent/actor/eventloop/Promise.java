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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This is starting to stray away from promises as "pure async" functions and
 * towards them being greedy co-routines that will only yield when they cannot
 * progress further. Whilst this reduces apparent parallelism, by eagerly
 * processing promises we completely avoid context switching, unnecessary
 * allocations etc. which should greatly improve cache coherence and avoid
 * saturating the real work queues necessary to communicate between threads.
 *
 * At some point, it would be nice for this style to work with actors on a
 * multi-threaded promise event loop.
 *
 * The guarantees of these promises can be interpreted as follows:
 *   - Operations in a promise are NOT guaranteed to be executed after the
 *     call to create them. They may be executed inline, asynchronously, or
 *     fully after the call has completed.
 *   - Chained promises are guaranteed to be executed in order and receive the
 *     result of the previous operation.
 */
@NotThreadSafe
public abstract class Promise<V> {
    private static final Logger LOG = LoggerFactory.getLogger(Promise.class);

    @CheckReturnValue
    public static <V> Promise<V> of(V value) {
        return new Precomputed<>(value);
    }

    @CheckReturnValue
    public static Promise<Void> done() {
        return Precomputed.DONE;
    }

    @CheckReturnValue
    public static <V> Promise<V> compute(final EventLoop eventLoop, final Supplier<V> job) {
        if (EventLoopThreadChecker.isOnThread(eventLoop)) {
            return new Precomputed<>(job.get());
        } else {
            final Computed<V> promise = new Computed<>();
            eventLoop.submit(() -> {
                final V v = job.get();
                promise.complete(v);
            });
            return promise;
        }
    }

    @CheckReturnValue
    public static <V> Promise<V> computeAsync(final EventLoop eventLoop, final Supplier<Promise<V>> jobAsync) {
        if (EventLoopThreadChecker.isOnThread(eventLoop)) {
            return jobAsync.get();
        } else {
            final Computed<V> promise = new Computed<>();
            eventLoop.submit(() -> {
                Promise<V> otherP = jobAsync.get();
                otherP.thenDefer(v -> promise.complete(v));
            });
            return promise;
        }
    }

    @CheckReturnValue
    public static <V> Promise<V> promise(final Consumer<Consumer<V>> promiser) {
        final Computed<V> promise = new Computed<>();
        promiser.accept(promise::complete);
        return promise;
    }

    public abstract void thenDefer(final Consumer<V> job);

    @CheckReturnValue
    public abstract Promise<V> thenRun(final Consumer<V> job);

    @CheckReturnValue
    public abstract <R> Promise<R> then(final Function<V, R> job);

    @CheckReturnValue
    public abstract <R> Promise<R> thenAsync(final Function<V, Promise<R>> jobAsync);

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
        private static final Promise<Void> DONE = new Precomputed<>(null);

        private final V value;
        Precomputed(V value) {
            this.value = value;
        }

        @Override
        public V await() {
            return value;
        }

        @Override
        public void thenDefer(final Consumer<V> job) {
            job.accept(value);
        }

        @Override
        @CheckReturnValue
        public Promise<V> thenRun(final Consumer<V> job) {
            job.accept(value);
            return this;
        }

        @Override
        @CheckReturnValue
        public <R> Promise<R> then(final Function<V, R> job) {
            return new Precomputed<>(job.apply(value));
        }

        @Override
        @CheckReturnValue
        public <R> Promise<R> thenAsync(final Function<V, Promise<R>> jobAsync) {
            return jobAsync.apply(value);
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
        public void thenDefer(final Consumer<V> job) {
            if (completed) {
                job.accept(value);
            } else {
                waiters.add(job);
            }
        }

        @Override
        @CheckReturnValue
        public Promise<V> thenRun(final Consumer<V> job) {
            final Computed<V> promise = new Computed<>();
            thenDefer(v -> {
                job.accept(v);
                promise.complete(v);
            });
            return promise;
        }

        @Override
        @CheckReturnValue
        public <R> Promise<R> then(final Function<V, R> job) {
            final Computed<R> promise = new Computed<>();
            thenDefer(v -> promise.complete(job.apply(v)));
            return promise;
        }

        @Override
        @CheckReturnValue
        public <R> Promise<R> thenAsync(final Function<V, Promise<R>> jobAsync) {
            final Computed<R> promise = new Computed<>();
            thenDefer(v -> jobAsync.apply(v).thenDefer(promise::complete));
            return promise;
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
