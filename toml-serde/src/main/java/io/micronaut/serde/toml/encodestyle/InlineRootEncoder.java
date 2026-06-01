/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.serde.toml.encodestyle;

import io.micronaut.core.annotation.Internal;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.serde.exceptions.SerdeException;

import java.io.IOException;
import java.util.Map;

import static io.micronaut.serde.toml.encodestyle.TomlStyleRenderer.canRenderObjectProperty;
import static io.micronaut.serde.toml.encodestyle.TomlStyleRenderer.renderKeySegment;
import static io.micronaut.serde.toml.encodestyle.TomlStyleRenderer.renderString;

/**
 * Renderer for TOML inline output style.
 *
 * @since 3.0.1
 */
@Internal
public final class InlineRootEncoder {
    private InlineRootEncoder() {
    }

    /**
     * Append a complete inline-style TOML document.
     *
     * @param builder The target builder
     * @param value The root TOML value
     * @throws IOException If the root value cannot be rendered
     */
    public static void appendInlineDocument(StringBuilder builder, JsonNode value) throws IOException {
        if (!value.isObject()) {
            throw new SerdeException("TOML root value must be an object");
        }
        for (Map.Entry<String, JsonNode> entry : value.entries()) {
            JsonNode entryValue = entry.getValue();
            if (!canRenderObjectProperty(entryValue)) {
                continue;
            }
            builder.append(renderKeySegment(entry.getKey()))
                .append(" = ")
                .append(renderInlineValue(entryValue))
                .append('\n');
        }
    }

    /**
     * Render a TOML value using inline syntax.
     * <a href="https://toml.io/en/v1.0.0#inline-table">TOML inline Table Spec</a>
     * used also by the Table Style function only with scalar values.
     *
     * @param value The value to render
     * @return The TOML inline representation
     * @throws IOException If the value cannot be represented in TOML (e.g. a null value)
     */
    public static String renderInlineValue(JsonNode value) throws IOException {
        if (value.isString()) {
            return renderString(value.getStringValue());
        }
        if (value.isNumber()) {
            return renderNumber(value.getNumberValue());
        }
        if (value.isBoolean()) {
            return Boolean.toString(value.getBooleanValue());
        }
        if (value.isNull()) {
            throw new SerdeException("TOML has no null literal; cannot encode a null value");
        }
        if (value.isArray()) {
            StringBuilder builder = new StringBuilder("[");
            int index = 0;
            for (JsonNode entry : value.values()) {
                if (index++ > 0) {
                    builder.append(", ");
                }
                builder.append(renderInlineValue(entry));
            }
            return builder.append(']').toString();
        }
        if (value.isObject()) {
            StringBuilder builder = new StringBuilder("{");
            int index = 0;
            for (Map.Entry<String, JsonNode> entry : value.entries()) {
                JsonNode entryValue = entry.getValue();
                if (!canRenderObjectProperty(entryValue)) {
                    continue;
                }
                if (index++ > 0) {
                    builder.append(", ");
                }
                builder.append(renderKeySegment(entry.getKey()))
                    .append(" = ")
                    .append(renderInlineValue(entryValue));
            }
            return builder.append('}').toString();
        }
        throw new IllegalStateException("Unknown TOML value: " + value);
    }

    /**
     * Renders numeric values per the TOML v1.0.0 Float specification.
     */
    private static String renderNumber(Number value) {
        if (value instanceof Float floatValue) {
            return renderFloat(floatValue);
        }
        if (value instanceof Double doubleValue) {
            return renderDouble(doubleValue);
        }
        return value.toString();
    }

    private static String renderFloat(float value) {
        if (Float.isNaN(value)) {
            return "nan";
        }
        if (value == Float.POSITIVE_INFINITY) {
            return "inf";
        }
        if (value == Float.NEGATIVE_INFINITY) {
            return "-inf";
        }
        return Float.toString(value);
    }

    private static String renderDouble(double value) {
        if (Double.isNaN(value)) {
            return "nan";
        }
        if (value == Double.POSITIVE_INFINITY) {
            return "inf";
        }
        if (value == Double.NEGATIVE_INFINITY) {
            return "-inf";
        }
        return Double.toString(value);
    }
}
