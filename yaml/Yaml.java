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

import static com.vaticle.typedb.common.util.Objects.className;

public interface Yaml {

    static Yaml load(String yaml) {
        return wrap(new org.yaml.snakeyaml.Yaml().load(yaml));
    }

    static Yaml load(Path filePath) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(filePath.toFile());
        return wrap(new org.yaml.snakeyaml.Yaml().load(inputStream));
    }

    static Yaml wrap(Object yamlCompatible) {
        if (yamlCompatible instanceof java.util.Map) return Map.wrap((java.util.Map<String, Object>) yamlCompatible);
        else if (yamlCompatible instanceof java.util.List) return List.wrap((java.util.List<Object>) yamlCompatible);
        else return Primitive.wrap(yamlCompatible);
    }

    default boolean isMap() {
        return false;
    }

    default Map asMap() {
        throw new ClassCastException(String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                className(Map.class)));
    }

    default boolean isList() {
        return false;
    }

    default List asList() {
        throw new ClassCastException(String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                className(List.class)));
    }

    default boolean isPrimitive() {
        return false;
    }

    default Primitive asPrimitive() {
        throw new ClassCastException(String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                className(Primitive.class)));
    }

    class Map extends java.util.LinkedHashMap<String, Yaml> implements Yaml {

        static Map wrap(java.util.Map<String, Object> map) {
            Map mapYaml = new Map();
            for (String key : map.keySet()) {
                mapYaml.put(key, Yaml.wrap(map.get(key)));
            }
            return mapYaml;
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

    class List extends ArrayList<Yaml> implements Yaml {

        static List wrap(java.util.List<Object> list) {
            List listYaml = new List();
            for (Object e : list) {
                listYaml.add(Yaml.wrap(e));
            }
            return listYaml;
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

    class Primitive implements Yaml {

        private final Object object;

        private Primitive(Object object) {
            this.object = object;
        }

        public static Primitive wrap(Object obj) {
            if (!(obj instanceof String || obj instanceof Integer || obj instanceof Float || obj instanceof Boolean)) {
                throw new ClassCastException("Type '" + className(obj.getClass()) + "' is not a known yaml primitive.");
            }
            return new Primitive(obj);
        }

        @Override
        public boolean isPrimitive() {
            return true;
        }

        @Override
        public Primitive asPrimitive() {
            return this;
        }

        public boolean isString() {
            return object instanceof String;
        }

        public String asString() {
            if (!isString()) {
                throw new ClassCastException(String.format("Illegal cast from '%s' to '%s'.",
                        className(getClass()), className(String.class)));
            }
            return (String) object;
        }

        public boolean isInt() {
            return object instanceof Integer;
        }

        public int asInt() {
            if (!isInt()) {
                throw new ClassCastException(String.format("Illegal cast from '%s' to '%s'.",
                        className(getClass()), className(Integer.class)));
            }
            return (Integer) object;
        }

        public boolean isFloat() {
            return object instanceof Float;
        }

        public float asFloat() {
            if (!isFloat()) {
                throw new ClassCastException(String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                        className(Float.class)));
            }
            return (Float) object;
        }

        public boolean isBoolean() {
            return object instanceof Boolean;
        }

        public boolean asBoolean() {
            if (!isBoolean()) {
                throw new ClassCastException(String.format("Illegal cast from '%s' to '%s'.", className(getClass()),
                        className(Boolean.class)));
            }
            return (Boolean) object;
        }

        @Override
        public String toString() {
            return object + "[" + className(object.getClass()) + "]";
        }
    }
}
