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
import java.util.LinkedHashMap;

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
        else if (yaml instanceof java.lang.Integer) return new Int((java.lang.Integer) yaml);
        else if (yaml instanceof java.lang.Float) return new Float((java.lang.Float) yaml);
        else if (yaml instanceof java.lang.Boolean) return new Boolean((java.lang.Boolean) yaml);
        else throw new IllegalStateException();
    }

    public boolean isMap() {
        return false;
    }

    public Map asMap() {
        throw new ClassCastException(java.lang.String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                className(Map.class)));
    }

    public boolean isList() {
        return false;
    }

    public List asList() {
        throw new ClassCastException(java.lang.String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                className(List.class)));
    }

    public boolean isString() {
        return false;
    }

    public String asString() {
        throw new ClassCastException(java.lang.String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                className(String.class)));
    }

    public boolean isInt() {
        return false;
    }

    public Int asInt() {
        throw new ClassCastException(java.lang.String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                className(Int.class)));
    }

    public boolean isFloat() {
        return false;
    }

    public Float asFloat() {
        throw new ClassCastException(java.lang.String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                className(Float.class)));
    }

    public boolean isBoolean() {
        return false;
    }

    public Boolean asBoolean() {
        throw new ClassCastException(java.lang.String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                className(Boolean.class)));
    }

    public static class Map extends Yaml {

        private final java.util.Map<java.lang.String, Yaml> map;

        private Map(java.util.Map<java.lang.String, Yaml> map) {
            this.map = map;
        }

        private static Map wrap(java.util.Map<java.lang.String, Object> yamlMap) {
            java.util.Map<java.lang.String, Yaml> map = new LinkedHashMap<>();
            for (java.lang.String key : yamlMap.keySet()) {
                map.put(key, Yaml.wrap(map.get(key)));
            }
            return new Map(map);
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

        static List wrap(java.util.List<Object> list) {
            java.util.List<Yaml> yamlList = new ArrayList<>();
            for (Object e : list) {
                yamlList.add(Yaml.wrap(e));
            }
            return new List(yamlList);
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

    static class String extends Yaml {

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

    static class Int extends Yaml {

        private final int value;

        private Int(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public boolean isInt() {
            return true;
        }

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

        public boolean isFloat() {
            return true;
        }

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

        public boolean isBoolean() {
            return true;
        }

        public Boolean asBoolean() {
            return this;
        }

        @Override
        public java.lang.String toString() {
            return value + "[boolean]";
        }
    }
}
