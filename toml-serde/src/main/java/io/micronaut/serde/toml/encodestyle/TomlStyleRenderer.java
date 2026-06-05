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

/**
 * Shared TOML text rendering helpers used by both table and inline output styles.
 *
 * <p>String/character are delegated now {@link StringOutputUtil}, a copy of
 * the upstream {@code jackson-dataformat-toml} utility.
 *
 * @see <a href="https://toml.io/en/v1.0.0#keys">TOML v1.0.0 Keys</a>
 * @see <a href="https://toml.io/en/v1.0.0#string">TOML v1.0.0 String</a>
 * @since 3.0.1
 */
@Internal
final class TomlStyleRenderer {

    private TomlStyleRenderer() {
    }

    static String renderKeySegment(String key) {
        if ((StringOutputUtil.categorize(key) & StringOutputUtil.UNQUOTED_KEY) != 0) {
            return key;
        }
        // overlapping byte
        return renderString(key);
    }

    static boolean canRenderObjectProperty(JsonNode value) {
        return !value.isNull();
    }

    /**
     * Reference to the <a href="https://toml.io/en/v1.0.0#string">TOML v1.0.0 String specification</a>.
     */
    static String renderString(String value) {
        if ((StringOutputUtil.categorize(value) & StringOutputUtil.LITERAL_STRING ) != 0) {
            return "'" + value + "'";
        }
        return "\"" + escapeBasicString(value) + "\"";
    }

    /**
     * Escapes a value for a TOML basic string, delegating per-character escaping to
     * {@link StringOutputUtil#getBasicStringEscape(char)}.
     *
     * @see <a href="https://toml.io/en/v1.0.0#string">TOML v1.0.0 String — Basic String</a>
     */
    private static String escapeBasicString(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 2);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String escape = StringOutputUtil.getBasicStringEscape(c);
            // determination
            if (escape != null) {
                builder.append(escape);
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
