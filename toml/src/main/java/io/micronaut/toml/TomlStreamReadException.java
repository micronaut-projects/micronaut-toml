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

import io.micronaut.core.annotation.Internal;

import java.io.IOException;

/**
 * TOML Stream Read Exception.
 * @author Jonas Konrad
 * @since 1.0.0
 */
@Internal
public final class TomlStreamReadException
        extends IOException {
    private static final int MAX_SNIPPET_LENGTH = 120;

    private final TomlLocation loc;

    /**
     *
     * @param msg Exception Message
     * @param loc TOML Location
     */
    TomlStreamReadException(String msg, TomlLocation loc) {
        super(msg);
        this.loc = loc;
    }

    /**
     *
     * @param msg Exception Message
     * @param loc TOML Location
     * @param rootCause Root Cause
     */
    TomlStreamReadException(String msg, TomlLocation loc, Throwable rootCause) {
        super(msg, rootCause);
        this.loc = loc;
    }

    /**
     *
     * @return Exception message.
     */
    public String getOriginalMessage() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        // adapted from jackson JsonProcessingException
        String msg = super.getMessage();
        if (msg == null) {
            msg = "N/A";
        }
        if (loc != null) {
            StringBuilder sb = new StringBuilder(100);
            sb.append(msg);
            sb.append('\n');
            sb.append(" at ");
            sb.append("line: ").append(loc.line);
            sb.append(", column: ").append(loc.column);

            int snippetStart = (int) loc.charPosition;
            int snippetEnd = (int) loc.charPosition;
            if (snippetStart > 0) {
                snippetStart = loc.content.lastIndexOf('\n', snippetStart - 1) + 1;
            }
            snippetEnd = loc.content.indexOf('\n', snippetEnd);
            if (snippetEnd == -1) {
                snippetEnd = loc.content.length();
            }
            if (snippetEnd - snippetStart > MAX_SNIPPET_LENGTH) {
                snippetStart = Math.max(snippetStart, (int) loc.charPosition - MAX_SNIPPET_LENGTH / 2);
                snippetEnd = Math.min(snippetEnd, snippetStart + MAX_SNIPPET_LENGTH);
            }
            String snippet = loc.content.substring(snippetStart, snippetEnd);
            snippet = snippet.replaceAll("[^\\x20-\\x7E]", ""); // remove non-ascii and control chars
            sb.append('\n').append(snippet).append('\n');
            for (int i = 0; i < loc.charPosition - snippetStart; i++) {
                sb.append(' ');
            }
            sb.append("^-- near here");

            msg = sb.toString();
        }
        return msg;
    }

    static class ErrorContext {
        private final String input;

        ErrorContext(String input) {
            this.input = input;
        }

        ErrorBuilder atPosition(Lexer lexer) {
            return new ErrorBuilder(lexer);
        }

        class ErrorBuilder {
            private final TomlLocation location;

            ErrorBuilder(Lexer lexer) {
                this.location = new TomlLocation(
                        input,
                        lexer.getCharPos(),
                        lexer.getLine() + 1,
                        lexer.getColumn() + 1
                );
            }

            TomlStreamReadException unexpectedToken(TomlToken actual, String expected) {
                return new TomlStreamReadException(
                        "Unexpected token: Got " + actual + ", expected " + expected,
                        location
                );
            }

            TomlStreamReadException generic(String message) {
                return new TomlStreamReadException(message, location);
            }

            TomlStreamReadException invalidNumber(NumberFormatException cause) {
                return new TomlStreamReadException("Invalid number representation", location, cause);
            }
        }
    }

    private static class TomlLocation {
        final String content;
        final long charPosition;
        final int line; // 1-based
        final int column; // 1-based

        TomlLocation(String content, long charPosition, int line, int column) {
            this.content = content;
            this.charPosition = charPosition;
            this.line = line;
            this.column = column;
        }
    }
}
