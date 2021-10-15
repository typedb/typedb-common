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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Yaml {

    private final Object object;

    private Yaml(Object yamlObject) {
        object = yamlObject;
    }

    public static Yaml create(String yaml) {
        return new Yaml(new org.yaml.snakeyaml.Yaml().<Object>load(yaml));
    }

    public boolean isMap() {
        return object instanceof Map;
    }

    public LinkedHashMap<String, Yaml> asMap() {
        LinkedHashMap<String, Object> mapObject = (LinkedHashMap<String, Object>) object;
        LinkedHashMap<String, Yaml> mapYaml = new LinkedHashMap<>();
        for (String key : mapObject.keySet()) {
            mapYaml.put(key, new Yaml(mapObject.get(key)));
        }
        return mapYaml;
    }

    public boolean isList() {
        return object instanceof List;
    }

    public List<Yaml> asList() {
        List<Object> listObject = (List<Object>) object;
        List<Yaml> listYaml = new ArrayList<>();
        for (Object e : listObject) {
            listYaml.add(new Yaml(e));
        }
        return listYaml;
    }

    public boolean isValue() {
        return object instanceof String;
    }

    public String asString() {
        return (String) object;
    }

    public Boolean asBoolean() {
        return (Boolean) object;
    }

}
