/*
 * Copyright (C) 2021 Vaticle
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

package com.vaticle.typedb.common.yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.vaticle.typedb.common.util.Objects.className;

public abstract class Yaml {

    public static Yaml load(java.lang.String yaml) {
        return wrap(new org.yaml.snakeyaml.Yaml().load(yaml));
    }

    public static Yaml load(Path filePath) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(filePath.toFile());
        return wrap(new org.yaml.snakeyaml.Yaml().load(inputStream));
    }

    private static Yaml wrap(Object yaml) {
        if (yaml instanceof java.util.Map) {
            assert ((java.util.Map<Object, Object>) yaml).keySet().stream().allMatch(key -> key instanceof java.lang.String);
            return Map.wrap((java.util.Map<java.lang.String, Object>) yaml);
        } else if (yaml instanceof java.util.List) return List.wrap((java.util.List<Object>) yaml);
        else if (yaml instanceof java.lang.String) return new String((java.lang.String) yaml);
        else if (yaml instanceof java.lang.Integer) return new Int((int) yaml);
        else if (yaml instanceof java.lang.Float) return new Float((float) yaml);
        else if (yaml instanceof java.lang.Boolean) return new Boolean((boolean) yaml);
        else throw new IllegalStateException();
    }

    public boolean isMap() {
        return false;
    }

    public Map asMap() {
        throw classCastException(getClass(), Map.class);
    }

    public boolean isList() {
        return false;
    }

    public List asList() {
        throw classCastException(getClass(), List.class);
    }

    public boolean isString() {
        return false;
    }

    public String asString() {
        throw classCastException(getClass(), String.class);
    }

    public boolean isInt() {
        return false;
    }

    public Int asInt() {
        throw classCastException(getClass(), Int.class);
    }

    public boolean isFloat() {
        return false;
    }

    public Float asFloat() {
        throw classCastException(getClass(), Float.class);
    }

    public boolean isBoolean() {
        return false;
    }

    public Boolean asBoolean() {
        throw classCastException(getClass(), Boolean.class);
    }

    private ClassCastException classCastException(Class<?> from, Class<?> to) {
        return new ClassCastException(java.lang.String.format("Illegal cast from '%s' to '%s'.", className(from),
                className(to)));
    }

    public static class Map extends Yaml {

        private final java.util.Map<java.lang.String, Yaml> map;

        public Map(java.util.Map<java.lang.String, Yaml> map) {
            this.map = map;
        }

        private static Map wrap(java.util.Map<java.lang.String, Object> source) {
            java.util.Map<java.lang.String, Yaml> map = new LinkedHashMap<>();
            for (java.lang.String key : source.keySet()) {
                if (source.get(key) == null) {
                    throw new IllegalArgumentException("Illegal null value for key: " + key + ".");
                }
                map.put(key, Yaml.wrap(source.get(key)));
            }
            return new Map(map);
        }

        public boolean containsKey(java.lang.String key) {
            return map.containsKey(key);
        }

        public Set<java.lang.String> keys() {
            return map.keySet();
        }

        public Yaml get(java.lang.String key) {
            return map.get(key);
        }

        public void put(java.lang.String key, Yaml value) {
            map.put(key, value);
        }

        public void forEach(BiConsumer<java.lang.String, Yaml> consumer) {
            map.forEach(consumer);
        }

        @Override
        public boolean isMap() {
            return true;
        }

        @Override
        public Map asMap() {
            return this;
        }
    }

    public static class List extends Yaml {

        private final java.util.List<Yaml> list;

        private List(java.util.List<Yaml> list) {
            this.list = list;
        }

        static List wrap(java.util.List<Object> source) {
            java.util.List<Yaml> yamlList = new ArrayList<>();
            for (Object e : source) {
                if (e == null) {
                    throw new IllegalArgumentException("Illegal null value encountered in list.");
                }
                yamlList.add(Yaml.wrap(e));
            }
            return new List(yamlList);
        }

        public Iterator<Yaml> iterator() {
            return list.iterator();
        }

        @Override
        public boolean isList() {
            return true;
        }

        @Override
        public List asList() {
            return this;
        }
    }

    public static class String extends Yaml {

        private final java.lang.String value;

        private String(java.lang.String string) {
            this.value = string;
        }

        public java.lang.String value() {
            return value;
        }

        @Override
        public boolean isString() {
            return true;
        }

        @Override
        public String asString() {
            return this;
        }

        @Override
        public java.lang.String toString() {
            return value + "[string]";
        }
    }

    public static class Int extends Yaml {

        private final int value;

        private Int(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        @Override
        public boolean isInt() {
            return true;
        }

        @Override
        public Int asInt() {
            return this;
        }

        @Override
        public java.lang.String toString() {
            return value + "[int]";
        }
    }

    public static class Float extends Yaml {

        private final float value;

        private Float(float value) {
            this.value = value;
        }

        public float value() {
            return value;
        }

        @Override
        public boolean isFloat() {
            return true;
        }

        @Override
        public Float asFloat() {
            return this;
        }

        @Override
        public java.lang.String toString() {
            return value + "[float]";
        }
    }

    public static class Boolean extends Yaml {

        private final boolean value;

        private Boolean(boolean value) {
            this.value = value;
        }

        public boolean value() {
            return value;
        }

        @Override
        public boolean isBoolean() {
            return true;
        }

        @Override
        public Boolean asBoolean() {
            return this;
        }

        @Override
        public java.lang.String toString() {
            return value + "[boolean]";
        }
    }
}
