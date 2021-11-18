package io.micronaut.toml;

import io.micronaut.core.annotation.Internal;

import java.io.IOException;

@Internal
public class TomlStreamReadException
        extends IOException {
    private final TomlLocation loc;

    TomlStreamReadException(String msg, TomlLocation loc) {
        super(msg);
        this.loc = loc;
    }

    TomlStreamReadException(String msg, TomlLocation loc, Throwable rootCause) {
        super(msg, rootCause);
        this.loc = loc;
    }

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
            sb.append(loc);
            msg = sb.toString();
        }
        return msg;
    }

    static class ErrorContext {
        ErrorContext() {
        }

        ErrorBuilder atPosition(Lexer lexer) {
            return new ErrorBuilder(lexer);
        }

        class ErrorBuilder {
            private final TomlLocation location;

            ErrorBuilder(Lexer lexer) {
                this.location = new TomlLocation(
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

            TomlStreamReadException outOfBounds(NumberFormatException cause) {
                return new TomlStreamReadException("Number out of bounds", location, cause);
            }
        }
    }

    private static class TomlLocation {
        final long charPosition;
        final int line; // 1-based
        final int column; // 1-based

        TomlLocation(long charPosition, int line, int column) {
            this.charPosition = charPosition;
            this.line = line;
            this.column = column;
        }
    }
}
