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
package io.micronaut.toml;

/**
 * TOML Token.
 *
 * @author Jonas Konrad
 * @since 1.0.0
 */
enum TomlToken {
    UNQUOTED_KEY,
    DOT_SEP,
    STRING,
    TRUE,
    FALSE,
    OFFSET_DATE_TIME,
    LOCAL_DATE_TIME,
    LOCAL_DATE,
    LOCAL_TIME,
    FLOAT,
    INTEGER,
    STD_TABLE_OPEN,
    STD_TABLE_CLOSE,
    INLINE_TABLE_OPEN,
    INLINE_TABLE_CLOSE,
    ARRAY_TABLE_OPEN,
    ARRAY_TABLE_CLOSE,
    ARRAY_OPEN,
    ARRAY_CLOSE,
    KEY_VAL_SEP,
    COMMA,
    /**
     * Whitespace token that is only permitted in arrays.
     */
    ARRAY_WS_COMMENT_NEWLINE
}
