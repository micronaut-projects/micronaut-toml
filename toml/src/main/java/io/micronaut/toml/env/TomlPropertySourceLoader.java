/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.toml.env;

import io.micronaut.context.env.AbstractPropertySourceLoader;
import io.micronaut.core.io.IOUtils;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.toml.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>A {@link io.micronaut.context.env.PropertySourceLoader} that reads <tt>application.toml</tt> files if they exist.</p>
 *
 * @author Jonas Konrad
 */
public class TomlPropertySourceLoader extends AbstractPropertySourceLoader {
    @Override
    public Set<String> getExtensions() {
        return Collections.singleton("toml");
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void processInput(String name, InputStream input, Map<String, Object> finalMap) throws IOException {
        // "A TOML file must be a valid UTF-8 encoded Unicode document."
        String text = IOUtils.readText(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)));
        JsonNode toml = Parser.parse(text);
        processMap(finalMap, (Map) unwrap(toml), "");
    }

    private Object unwrap(JsonNode value) {
        if (value.isNumber()) {
            return value.getNumberValue();
        } else if (value.isNull()) {
            return null;
        } else if (value.isBoolean()) {
            return value.getBooleanValue();
        } else if (value.isArray()) {
            List<Object> unwrapped = new ArrayList<>();
            value.values().forEach(v -> unwrapped.add(unwrap(v)));
            return unwrapped;
        } else if (value.isObject()) {
            Map<String, Object> unwrapped = new LinkedHashMap<>();
            value.entries().forEach(e -> unwrapped.put(e.getKey(), unwrap(e.getValue())));
            return unwrapped;
        } else {
            return value.getStringValue();
        }
    }
}
