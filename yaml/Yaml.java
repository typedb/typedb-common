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

    static Yaml create(String yaml) {
        return wrap(new org.yaml.snakeyaml.Yaml().load(yaml));
    }

    static Yaml create(Path filePath) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(filePath.toFile());
        return wrap(new org.yaml.snakeyaml.Yaml().load(inputStream));
    }

    static Yaml wrap(Object parsedYaml) {
        if (parsedYaml instanceof java.util.Map) return Map.create((java.util.Map<String, Object>) parsedYaml);
        else if (parsedYaml instanceof java.util.List) return List.create((java.util.List<Object>) parsedYaml);
        else return Primitive.create(parsedYaml);
    }

    default boolean isMap() {
        return false;
    }

    default Map asMap() {
        throw new ClassCastException();
    }

    default boolean isList() {
        return false;
    }

    default List asList() {
        throw new ClassCastException();
    }

    default boolean isPrimitive() {
        return false;
    }

    default Primitive asPrimitive() {
        throw new ClassCastException();
    }

    class Map extends java.util.LinkedHashMap<String, Yaml> implements Yaml {

        static Map create(java.util.Map<String, Object> map) {
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

        static List create(java.util.List<Object> list) {
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

        public static Primitive create(Object object) {
            assert object instanceof String || object instanceof Integer || object instanceof Float ||
                    object instanceof Boolean;
            return new Primitive(object);
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
            return (String) object;
        }

        public boolean isInt() {
            return object instanceof Integer;
        }

        public int asInt() {
            return (Integer) object;
        }

        public boolean isFloat() {
            return object instanceof Float;
        }

        public float asFloat() {
            return (Float) object;
        }

        public boolean isBoolean() {
            return object instanceof Boolean;
        }

        public boolean asBoolean() {
            return (Boolean) object;
        }

        @Override
        public String toString() {
            return object + "[" + className(object.getClass()) + "]";
        }
    }
}
