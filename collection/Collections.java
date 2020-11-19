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

package grakn.common.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Collections {

    @SafeVarargs
    public static <K, V> Map<K, V> map(Pair<K, V>... pairs) {
        Map<K, V> map = new HashMap<>();
        for (Pair<K, V> tuple : pairs) {
            map.put(tuple.first(), tuple.second());
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    public static <K, V> Map<K, V> map(Map<K, V> map) {
        return java.util.Collections.unmodifiableMap(map);
    }

    @SafeVarargs
    public static <T> Set<T> set(T... elements) {
        return set(Arrays.asList(elements));
    }

    @SafeVarargs
    public static <T> Set<T> set(Collection<T>... collections) {
        Set<T> set = new HashSet<>();
        for (Collection<T> collection : collections) set.addAll(collection);
        return java.util.Collections.unmodifiableSet(set);
    }

    public static <T> Set<T> set(Collection<T> elements) {
        Set<T> set = new HashSet<>(elements);
        return java.util.Collections.unmodifiableSet(set);
    }

    @SafeVarargs
    public static <T> List<T> list(T... elements) {
        return java.util.Collections.unmodifiableList(Arrays.asList(elements));
    }

    public static <T> List<T> extend(Collection<T> collection, T... array) {
        List<T> combined = new ArrayList<>(collection);
        combined.addAll(Arrays.asList(array));
        return java.util.Collections.unmodifiableList(combined);
    }

    public static <T> List<T> copy(Collection<T> elements) {
        List<T> list = new ArrayList<>(elements);
        return java.util.Collections.unmodifiableList(list);
    }

    public static <T> List<T> concat(Collection<T>... collections) {
        List<T> combined = new ArrayList<>();
        for (Collection<T> collection : collections) {
            combined.addAll(collection);
        }
        return java.util.Collections.unmodifiableList(combined);
    }

    public static <A, B> Pair<A, B> pair(A first, B second) {
        return new Pair<>(first, second);
    }

    public static <A, B, C> Triple<A, B, C> triple(A first, B second, C third) {
        return new Triple<>(first, second, third);
    }
}
