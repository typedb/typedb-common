/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.common.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyIterator;
import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;
import static java.util.Collections.singletonList;
import static java.util.Collections.sort;
import static java.util.Collections.swap;

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
    public static <T> Set<T> set(T... items) {
        return set(Arrays.asList(items));
    }

    public static <T> Set<T> set(Collection<T> collection) {
        Set<T> set = new HashSet<>(collection);
        return java.util.Collections.unmodifiableSet(set);
    }

    @SafeVarargs
    public static <T> Set<T> set(Collection<T> collection, T item, T... items) {
        Set<T> combined = new HashSet<>(collection);
        combined.add(item);
        combined.addAll(Arrays.asList(items));
        return java.util.Collections.unmodifiableSet(combined);
    }

    @SafeVarargs
    public static <T> Set<T> set(Collection<T> collection, Collection<T>... collections) {
        Set<T> combined = new HashSet<>(collection);
        for (Collection<T> c : collections) combined.addAll(c);
        return java.util.Collections.unmodifiableSet(combined);
    }

    @SafeVarargs
    public static <T> List<T> list(T... items) {
        return java.util.Collections.unmodifiableList(Arrays.asList(items));
    }

    public static <T> List<T> list(Collection<T> collection) {
        List<T> list = new ArrayList<>(collection);
        return java.util.Collections.unmodifiableList(list);
    }

    @SafeVarargs
    public static <T> List<T> list(Collection<T> collection, T item, T... array) {
        List<T> combined = new ArrayList<>(collection);
        combined.add(item);
        combined.addAll(Arrays.asList(array));
        return java.util.Collections.unmodifiableList(combined);
    }

    @SafeVarargs
    public static <T> List<T> list(Collection<T> collection, Collection<T>... collections) {
        List<T> combined = new ArrayList<>(collection);
        for (Collection<T> c : collections) combined.addAll(c);
        return java.util.Collections.unmodifiableList(combined);
    }

    /**
     * We implement the C++ STL next_permutation method of lazily generating permutations
     */
    public static <T> Iterator<List<T>> permutations(Set<T> items) {
        if (items.size() == 0) return emptyIterator();
        else if (items.size() == 1) return singletonList(list(items.iterator().next())).iterator();

        // assign a comparable ordering over the items
        Map<Integer, T> mapping = new HashMap<>();
        List<T> sortedItems = items.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList());
        int[] itemKeys = new int[sortedItems.size()];
        for (int i = 0; i < sortedItems.size(); i++) {
            mapping.put(i, sortedItems.get(i));
            itemKeys[i] = i;
        }

        return new Iterator<List<T>>() {

            boolean hasNext = true;

            @Override
            public boolean hasNext() {
                if (hasNext) return true;

                // find the longest tail that is decreasing
                int tailIndex = itemKeys.length - 1;
                while (itemKeys[tailIndex] < itemKeys[tailIndex - 1]) {
                    tailIndex--;
                    if (tailIndex == 0) return false;
                }

                // swap the next element with the smallest element larger than it in the descending tail
                for (int swap = itemKeys.length - 1; swap >= tailIndex; swap--) {
                    if (itemKeys[swap] > itemKeys[tailIndex - 1]) {
                        swap(itemKeys, swap, tailIndex - 1);
                        break;
                    }
                }

                // reverse the tail to get it back into increasing order and generate lexicographically next permutation
                for (int i = tailIndex, j = itemKeys.length - 1; i < j; i++, j--) {
                    swap(itemKeys, i, j);
                }

                hasNext = true;
                return true;
            }

            private void swap(int[] arr, int i, int j) {
                int tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;
            }

            @Override
            public List<T> next() {
                if (!hasNext()) throw new NoSuchElementException();
                // convert the keys back into the items
                List<T> permutation = new ArrayList<>(items.size());
                for (int index : itemKeys) {
                    permutation.add(mapping.get(index));
                }
                hasNext = false;
                return permutation;
            }
        };
    }


    public static <T> List<List<T>> permutations(List<T> items) {
        if (items.size() == 0) return singletonList(emptyList());
        List<List<T>> permutations = singletonList(singletonList(items.get(0)));
        for (int permutationLength = 1; permutationLength < items.size(); permutationLength++) {
            T toInsert = items.get(permutationLength);
            List<List<T>> extendedPermutations = new ArrayList<>();
            for (List<T> permutation : permutations) {
                for (int insertIndex = 0; insertIndex < permutation.size(); insertIndex++) {
                    List<T> copy = new ArrayList<>(permutation);
                    copy.add(insertIndex, toInsert);
                    extendedPermutations.add(copy);
                }
                List<T> addedAtEnd = new ArrayList<>(permutation);
                addedAtEnd.add(toInsert);
                extendedPermutations.add(addedAtEnd);
            }
            permutations = extendedPermutations;
        }
        return permutations;
    }

    public static <A, B> Pair<A, B> pair(A first, B second) {
        return new Pair<>(first, second);
    }

    public static <A, B, C> Triple<A, B, C> triple(A first, B second, C third) {
        return new Triple<>(first, second, third);
    }

    public static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        Set<T> minSet;
        Set<T> maxSet;
        if (set1.size() < set2.size()) {
            minSet = set1;
            maxSet = set2;
        } else {
            minSet = set2;
            maxSet = set1;
        }
        Set<T> intersection = new HashSet<>();
        for (T elem : minSet) {
            if (maxSet.contains(elem)) intersection.add(elem);
        }
        return intersection;
    }

    /**
     * Optimised set intersection detection when using sorted sets
     */
    public static <T extends Comparable<T>> boolean hasIntersection(NavigableSet<T> set1, NavigableSet<T> set2) {
        NavigableSet<T> active = set1;
        NavigableSet<T> other = set2;
        if (active.isEmpty()) return false;
        T currentKey = active.first();
        while (currentKey != null) {
            T otherKey = other.ceiling(currentKey);
            if (otherKey != null && otherKey.equals(currentKey)) return true;
            currentKey = otherKey;
            NavigableSet<T> tmp = other;
            other = active;
            active = tmp;
        }
        return false;
    }
}
