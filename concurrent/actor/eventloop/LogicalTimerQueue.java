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

import java.util.PriorityQueue;

public class LogicalTimerQueue<V> {
    private final PriorityQueue<LogicalTimedItem> timerQueue = new PriorityQueue<>();

    private long queueCounter; // Used to break ties

    public class LogicalTimedItem implements Comparable<LogicalTimedItem> {
        private final long version;
        private long expireAtMillis;
        private final V value;
        private boolean cancelled = false;

        public LogicalTimedItem(long expireAtMillis, V value) {
            this.expireAtMillis = expireAtMillis;
            this.value = value;
            version = queueCounter++;
        }

        @Override
        public int compareTo(LogicalTimedItem other) {
            if (expireAtMillis < other.expireAtMillis) {
                return -1;
            } else if (expireAtMillis > other.expireAtMillis) {
                return 1;
            } else {
                return Long.compare(version, other.version); // First timer wins a tie
            }
        }

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    public LogicalTimedItem offer(long expireAtMillis, V value) {
        LogicalTimedItem item = new LogicalTimedItem(expireAtMillis, value);
        timerQueue.add(item);
        return item;
    }

    public long timeToNext(long currentMillis) {
        LogicalTimedItem timer = peekToNextReady();
        if (timer == null) return Long.MAX_VALUE;
        return timer.expireAtMillis - currentMillis;
    }

    public V poll(long currentMillis) {
        LogicalTimedItem timer = peekToNextReady();
        if (timer == null) return null;
        if (timer.expireAtMillis > currentMillis) return null;
        timerQueue.poll();
        return timer.value;
    }

    private LogicalTimedItem peekToNextReady() {
        LogicalTimedItem item;
        while ((item = timerQueue.peek()) != null && item.isCancelled()) {
            timerQueue.poll();
        }
        return item;
    }
}
