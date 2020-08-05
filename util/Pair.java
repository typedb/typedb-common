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
 *
 */

package grakn.common.util;

import java.util.AbstractMap;

public class Pair<T, U> {

    private final AbstractMap.SimpleImmutableEntry<T, U> data;

    public Pair(T key, U value) {
        data = new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    public T first() { return data.getKey();}

    public U second() { return data.getValue();}

    @Override
    public String toString() { return data.toString();}

    @Override
    public boolean equals(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        Pair p = (Pair) obj;
        return data.equals(p.data);
    }

    @Override
    public int hashCode() { return data.hashCode();}
}