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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.json.tree.JsonNode;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TOML parser class. Not stable API, internal use only.
 *
 * Note: This class is adapted from
 * <a href='https://github.com/FasterXML/jackson-dataformats-text/blob/2.14/toml'>jackson-dataformats-text</a>, also
 * built by me (Jonas Konrad).
 *
 * @author Jonas Konrad
 */
@Internal
public final class Parser {
    private final TomlStreamReadException.ErrorContext errorContext;
    private final Lexer lexer;

    private TomlToken next;

    private Parser(
            TomlStreamReadException.ErrorContext errorContext,
            Reader input
    ) throws IOException {
        this.errorContext = errorContext;
        this.lexer = new Lexer(input, errorContext);
        lexer.prohibitInternalBufferAllocate = false;
        this.next = lexer.yylex();
    }

    public static JsonNode parse(String input) throws IOException {
        Parser parser = new Parser(new TomlStreamReadException.ErrorContext(input), new StringReader(input));
        return parser.parse();
    }

    private TomlToken peek() throws TomlStreamReadException {
        TomlToken here = this.next;
        if (here == null) {
            throw errorContext.atPosition(lexer).generic("Premature end of file");
        }
        return here;
    }

    /**
     * Note: Polling also lexes the next token, so methods like {@link Lexer#yytext()} will not work afterwards.
     */
    private TomlToken poll(int nextState) throws IOException {
        TomlToken here = peek();
        lexer.yybegin(nextState);
        next = lexer.yylex();
        return here;
    }

    private void pollExpected(TomlToken expected, int nextState) throws IOException {
        TomlToken actual = poll(nextState);
        if (actual != expected) {
            throw errorContext.atPosition(lexer).unexpectedToken(actual, expected.toString());
        }
    }

    public JsonNode parse() throws IOException {
        TomlObjectBuilder root = new TomlObjectBuilder();
        TomlObjectBuilder currentTable = root;
        while (next != null) {
            TomlToken token = peek();
            if (token == TomlToken.UNQUOTED_KEY || token == TomlToken.STRING) {
                parseKeyVal(currentTable, Lexer.EXPECT_EOL);
            } else if (token == TomlToken.STD_TABLE_OPEN) {
                pollExpected(TomlToken.STD_TABLE_OPEN, Lexer.EXPECT_INLINE_KEY);
                FieldRef fieldRef = parseAndEnterKey(root, true);
                currentTable = getOrCreateObject(fieldRef.object, fieldRef.key);
                if (currentTable.defined) {
                    throw errorContext.atPosition(lexer).generic("Table redefined");
                }
                currentTable.defined = true;
                pollExpected(TomlToken.STD_TABLE_CLOSE, Lexer.EXPECT_EOL);
            } else if (token == TomlToken.ARRAY_TABLE_OPEN) {
                pollExpected(TomlToken.ARRAY_TABLE_OPEN, Lexer.EXPECT_INLINE_KEY);
                FieldRef fieldRef = parseAndEnterKey(root, true);
                TomlArrayBuilder array = getOrCreateArray(fieldRef.object, fieldRef.key);
                if (array.closed) {
                    throw errorContext.atPosition(lexer).generic("Array already finished");
                }
                currentTable = array.addObject();
                pollExpected(TomlToken.ARRAY_TABLE_CLOSE, Lexer.EXPECT_EOL);
            } else {
                throw errorContext.atPosition(lexer).unexpectedToken(token, "key or table");
            }
        }
        int eofState = lexer.yystate();
        if (eofState != Lexer.EXPECT_EXPRESSION && eofState != Lexer.EXPECT_EOL) {
            throw errorContext.atPosition(lexer).generic("EOF in wrong state");
        }
        return root.build();
    }

    private FieldRef parseAndEnterKey(
            TomlObjectBuilder outer,
            boolean forTable
    ) throws IOException {
        TomlObjectBuilder node = outer;
        while (true) {
            if (node.closed) {
                throw errorContext.atPosition(lexer).generic("Object already closed");
            }
            if (!forTable) {
                /* "Dotted keys create and define a table for each key part before the last one, provided that such
                 * tables were not previously created." */
                node.defined = true;
            }

            TomlToken partToken = peek();
            String part;
            if (partToken == TomlToken.STRING) {
                part = lexer.textBuffer.toString();
            } else if (partToken == TomlToken.UNQUOTED_KEY) {
                part = lexer.yytext();
            } else {
                throw errorContext.atPosition(lexer).unexpectedToken(partToken, "quoted or unquoted key");
            }
            pollExpected(partToken, Lexer.EXPECT_INLINE_KEY);
            if (peek() != TomlToken.DOT_SEP) {
                return new FieldRef(node, part);
            }
            pollExpected(TomlToken.DOT_SEP, Lexer.EXPECT_INLINE_KEY);

            TomlNodeBuilder existing = node.get(part);
            if (existing == null) {
                node = node.putObject(part);
            } else if (existing instanceof TomlObjectBuilder) {
                node = (TomlObjectBuilder) existing;
            } else if (existing instanceof TomlArrayBuilder) {
                /* "Any reference to an array of tables points to the most recently defined table element of the array.
                 * This allows you to define sub-tables, and even sub-arrays of tables, inside the most recent table."
                 *
                 * I interpret this somewhat broadly: I accept such references even if there were unrelated tables
                 * in between, and I accept them for simple dotted keys as well (not just for tables). These cases don't
                 * seem to be covered by the specification.
                 */
                TomlArrayBuilder array = (TomlArrayBuilder) existing;
                if (array.closed) {
                    throw errorContext.atPosition(lexer).generic("Array already closed");
                }
                // Only arrays declared by array tables are not closed, and those are always arrays of objects.
                node = (TomlObjectBuilder) array.get(array.size() - 1);
            } else {
                throw errorContext.atPosition(lexer).generic("Path into existing non-object value of type " + existing.getNodeType());
            }
        }
    }

    private TomlNodeBuilder parseValue(int nextState) throws IOException {
        TomlToken firstToken = peek();
        switch (firstToken) {
            case STRING:
                String text = lexer.textBuffer.toString();
                pollExpected(TomlToken.STRING, nextState);
                return new Scalar(JsonNode.createStringNode(text));
            case TRUE:
                pollExpected(TomlToken.TRUE, nextState);
                return new Scalar(JsonNode.createBooleanNode(true));
            case FALSE:
                pollExpected(TomlToken.FALSE, nextState);
                return new Scalar(JsonNode.createBooleanNode(false));
            case OFFSET_DATE_TIME:
            case LOCAL_DATE_TIME:
            case LOCAL_DATE:
            case LOCAL_TIME:
                return new Scalar(parseDateTime(nextState));
            case FLOAT:
                return new Scalar(parseFloat(nextState));
            case INTEGER:
                return new Scalar(parseInt(nextState));
            case ARRAY_OPEN:
                return parseArray(nextState);
            case INLINE_TABLE_OPEN:
                return parseInlineTable(nextState);
            default:
                throw errorContext.atPosition(lexer).unexpectedToken(firstToken, "value");
        }
    }

    private JsonNode parseDateTime(int nextState) throws IOException {
        String text = lexer.yytext();
        TomlToken token = poll(nextState);
        // the time-delim index can be [Tt ]. java.time supports only [Tt]
        if ((token == TomlToken.LOCAL_DATE_TIME || token == TomlToken.OFFSET_DATE_TIME) && text.charAt(10) == ' ') {
            text = text.substring(0, 10) + 'T' + text.substring(11);
        }

        /*
        if (TomlReadFeature.PARSE_JAVA_TIME.enabledIn(options)) {
            Temporal value;
            if (token == TomlToken.LOCAL_DATE) {
                value = LocalDate.parse(text);
            } else if (token == TomlToken.LOCAL_TIME) {
                value = LocalTime.parse(text);
            } else {
                if (token == TomlToken.LOCAL_DATE_TIME) {
                    value = LocalDateTime.parse(text);
                } else if (token == TomlToken.OFFSET_DATE_TIME) {
                    value = OffsetDateTime.parse(text);
                } else {
                    VersionUtil.throwInternal();
                    throw new AssertionError();
                }
            }
            return factory.pojoNode(value);
        } else {

         */
        return JsonNode.createStringNode(text);
        //}
    }

    private JsonNode parseInt(int nextState) throws IOException {
        char[] buffer = lexer.getTextBuffer();
        int start = lexer.getTextBufferStart();
        int length = lexer.getTextBufferEnd() - lexer.getTextBufferStart();
        for (int i = 0; i < length; i++) {
            if (buffer[start + i] == '_') {
                // slow path to remove underscores
                buffer = new String(buffer, start, length).replace("_", "").toCharArray();
                start = 0;
                length = buffer.length;
                break;
            }
        }

        pollExpected(TomlToken.INTEGER, nextState);
        if (length > 2) {
            char baseChar = buffer[start + 1];
            if (baseChar == 'x' || baseChar == 'o' || baseChar == 'b') {
                start += 2;
                length -= 2;
                String text = new String(buffer, start, length);
                // note: we parse all these as unsigned. Hence the weird int limits.
                // hex
                if (baseChar == 'x') {
                    if (length <= 31 / 4) {
                        return JsonNode.createNumberNode(Integer.parseInt(text, 16));
                    } else if (length <= 63 / 4) {
                        return JsonNode.createNumberNode(Long.parseLong(text, 16));
                    } else {
                        return JsonNode.createNumberNode(new BigInteger(text, 16));
                    }
                }
                // octal
                if (baseChar == 'o') {
                    // this is a bit conservative, but who uses octal anyway?
                    if (length <= 31 / 3) {
                        return JsonNode.createNumberNode(Integer.parseInt(text, 8));
                    } else if (text.length() <= 63 / 3) {
                        return JsonNode.createNumberNode(Long.parseLong(text, 8));
                    } else {
                        return JsonNode.createNumberNode(new BigInteger(text, 8));
                    }
                }
                // binary
                assert baseChar == 'b';
                if (length <= 31) {
                    return JsonNode.createNumberNode(Integer.parseUnsignedInt(text, 2));
                } else if (length <= 63) {
                    return JsonNode.createNumberNode(Long.parseUnsignedLong(text, 2));
                } else {
                    return JsonNode.createNumberNode(new BigInteger(text, 2));
                }
            }
        }
        // decimal
        boolean negative;
        if (buffer[start] == '-') {
            start++;
            length--;
            negative = true;
        } else if (buffer[start] == '+') {
            start++;
            length--;
            negative = false;
        } else {
            negative = false;
        }
        String bufferString = new String(buffer, start, length);
        // adapted from JsonParserBase
        if (length <= 9) {
            int v = Integer.parseInt(bufferString);
            if (negative) {
                v = -v;
            }
            return JsonNode.createNumberNode(v);
        }
        if (length <= 18) {
            long v = Long.parseLong(bufferString);
            if (negative) {
                v = -v;
            }
            // Might still fit in int, need to check
            if ((int) v == v) {
                return JsonNode.createNumberNode((int) v);
            } else {
                return JsonNode.createNumberNode(v);
            }
        }
        return JsonNode.createNumberNode(new BigInteger(bufferString));
    }

    private JsonNode parseFloat(int nextState) throws IOException {
        String text = lexer.yytext().replace("_", "");
        pollExpected(TomlToken.FLOAT, nextState);
        if (text.endsWith("nan")) {
            return JsonNode.createNumberNode(Float.NaN);
        } else if (text.endsWith("inf")) {
            return JsonNode.createNumberNode(text.startsWith("-") ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY);
        } else {
            BigDecimal dec = new BigDecimal(text);
            return JsonNode.createNumberNode(dec);
        }
    }

    private TomlObjectBuilder parseInlineTable(int nextState) throws IOException {
        // inline-table = inline-table-open [ inline-table-keyvals ] inline-table-close
        // inline-table-keyvals = keyval [ inline-table-sep inline-table-keyvals ]
        pollExpected(TomlToken.INLINE_TABLE_OPEN, Lexer.EXPECT_INLINE_KEY);
        TomlObjectBuilder node = new TomlObjectBuilder();
        while (true) {
            TomlToken token = peek();
            if (token == TomlToken.INLINE_TABLE_CLOSE) {
                if (node.isEmpty()) {
                    break;
                } else {
                    // "A terminating comma (also called trailing comma) is not permitted after the last key/value pair
                    // in an inline table."
                    throw errorContext.atPosition(lexer).generic("Trailing comma not permitted for inline tables");
                }
            }
            parseKeyVal(node, Lexer.EXPECT_TABLE_SEP);
            TomlToken sepToken = peek();
            if (sepToken == TomlToken.INLINE_TABLE_CLOSE) {
                break;
            } else if (sepToken == TomlToken.COMMA) {
                pollExpected(TomlToken.COMMA, Lexer.EXPECT_INLINE_KEY);
            } else {
                throw errorContext.atPosition(lexer).unexpectedToken(sepToken, "comma or table end");
            }
        }
        pollExpected(TomlToken.INLINE_TABLE_CLOSE, nextState);
        node.closed = true;
        node.defined = true;
        return node;
    }

    private TomlArrayBuilder parseArray(int nextState) throws IOException {
        // array = array-open [ array-values ] ws-comment-newline array-close
        // array-values =  ws-comment-newline val ws-comment-newline array-sep array-values
        // array-values =/ ws-comment-newline val ws-comment-newline [ array-sep ]
        pollExpected(TomlToken.ARRAY_OPEN, Lexer.EXPECT_VALUE);
        TomlArrayBuilder node = new TomlArrayBuilder();
        while (true) {
            TomlToken token = peek();
            if (token == TomlToken.ARRAY_CLOSE) {
                break;
            }
            TomlNodeBuilder value = parseValue(Lexer.EXPECT_ARRAY_SEP);
            node.add(value);
            TomlToken sepToken = peek();
            if (sepToken == TomlToken.ARRAY_CLOSE) {
                break;
            } else if (sepToken == TomlToken.COMMA) {
                pollExpected(TomlToken.COMMA, Lexer.EXPECT_VALUE);
            } else {
                throw errorContext.atPosition(lexer).unexpectedToken(sepToken, "comma or array end");
            }
        }
        pollExpected(TomlToken.ARRAY_CLOSE, nextState);
        node.closed = true;
        return node;
    }

    private void parseKeyVal(TomlObjectBuilder target, int nextState) throws IOException {
        // keyval = key keyval-sep val
        FieldRef fieldRef = parseAndEnterKey(target, false);
        pollExpected(TomlToken.KEY_VAL_SEP, Lexer.EXPECT_VALUE);
        TomlNodeBuilder value = parseValue(nextState);
        if (fieldRef.object.has(fieldRef.key)) {
            throw errorContext.atPosition(lexer).generic("Duplicate key");
        }
        fieldRef.object.set(fieldRef.key, value);
    }

    private TomlObjectBuilder getOrCreateObject(TomlObjectBuilder node, String field) throws TomlStreamReadException {
        TomlNodeBuilder existing = node.get(field);
        if (existing == null) {
            return node.putObject(field);
        } else if (existing instanceof TomlObjectBuilder) {
            return (TomlObjectBuilder) existing;
        } else {
            throw errorContext.atPosition(lexer).generic("Path into existing non-object value of type " + existing.getNodeType());
        }
    }

    private TomlArrayBuilder getOrCreateArray(TomlObjectBuilder node, String field) throws TomlStreamReadException {
        TomlNodeBuilder existing = node.get(field);
        if (existing == null) {
            return node.putArray(field);
        } else if (existing instanceof TomlArrayBuilder) {
            return (TomlArrayBuilder) existing;
        } else {
            throw errorContext.atPosition(lexer).generic("Path into existing non-array value of type " + node.getNodeType());
        }
    }

    private static class FieldRef {
        final TomlObjectBuilder object;
        final String key;

        FieldRef(TomlObjectBuilder object, String key) {
            this.object = object;
            this.key = key;
        }
    }

    private interface TomlNodeBuilder {
        JsonNode build();

        String getNodeType();
    }

    private static class Scalar implements TomlNodeBuilder {
        private final JsonNode value;

        Scalar(JsonNode value) {
            this.value = value;
        }

        @Override
        public JsonNode build() {
            return value;
        }

        @Override
        public String getNodeType() {
            if (value.isNumber()) {
                return "number";
            } else if (value.isBoolean()) {
                return "boolean";
            } else if (value.isString()) {
                return "string";
            } else {
                return value.toString();
            }
        }
    }

    private static class TomlObjectBuilder implements TomlNodeBuilder {
        final Map<String, TomlNodeBuilder> nodes = new LinkedHashMap<>();
        boolean closed = false;
        boolean defined = false;

        @Override
        public JsonNode build() {
            return JsonNode.createObjectNode(nodes.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build())));
        }

        public TomlObjectBuilder putObject(String key) {
            TomlObjectBuilder child = new TomlObjectBuilder();
            nodes.put(key, child);
            return child;
        }

        @Nullable
        public TomlNodeBuilder get(String key) {
            return nodes.get(key);
        }

        public TomlArrayBuilder putArray(String key) {
            TomlArrayBuilder child = new TomlArrayBuilder();
            nodes.put(key, child);
            return child;
        }

        public boolean isEmpty() {
            return nodes.isEmpty();
        }

        public boolean has(String key) {
            return nodes.containsKey(key);
        }

        public void set(String key, TomlNodeBuilder value) {
            nodes.put(key, value);
        }

        @Override
        public String getNodeType() {
            return "table";
        }
    }

    private static class TomlArrayBuilder implements TomlNodeBuilder {
        final List<TomlNodeBuilder> nodes = new ArrayList<>();
        boolean closed = false;

        @Override
        public JsonNode build() {
            return JsonNode.createArrayNode(nodes.stream().map(TomlNodeBuilder::build).collect(Collectors.toList()));
        }

        public int size() {
            return nodes.size();
        }

        public TomlNodeBuilder get(int i) {
            return nodes.get(i);
        }

        public TomlObjectBuilder addObject() {
            TomlObjectBuilder child = new TomlObjectBuilder();
            nodes.add(child);
            return child;
        }

        @Override
        public String getNodeType() {
            return "array";
        }

        public void add(TomlNodeBuilder value) {
            nodes.add(value);
        }
    }
}
