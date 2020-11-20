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

import java.util.PriorityQueue;

public class ScheduledJobQueue<V> {
    private final PriorityQueue<Entry> queue = new PriorityQueue<>();
    private long queueCounter;

    public Entry offer(long expireAtMs, V value) {
        Entry item = new Entry(expireAtMs, value);
        queue.add(item);
        return item;
    }

    public V poll(long currentTimeMs) {
        Entry timer = peekToNextReady();
        if (timer == null) return null;
        if (timer.expireAtMs > currentTimeMs) return null;
        queue.poll();
        return timer.value;
    }

    public long timeToNext(long currentTimeMs) {
        Entry timer = peekToNextReady();
        if (timer == null) return Long.MAX_VALUE;
        return timer.expireAtMs - currentTimeMs;
    }

    private Entry peekToNextReady() {
        Entry item;
        while ((item = queue.peek()) != null && item.isCancelled()) {
            queue.poll();
        }
        return item;
    }

    public class Entry implements Comparable<Entry> {
        private final long version;
        private final long expireAtMs;
        private final V value;
        private boolean cancelled = false;

        public Entry(long expireAtMs, V value) {
            this.expireAtMs = expireAtMs;
            this.value = value;
            version = queueCounter++;
        }

        @Override
        public int compareTo(Entry other) {
            if (expireAtMs < other.expireAtMs) {
                return -1;
            } else if (expireAtMs > other.expireAtMs) {
                return 1;
            } else {
                return Long.compare(version, other.version);
            }
        }

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
