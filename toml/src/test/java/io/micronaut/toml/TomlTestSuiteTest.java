package io.micronaut.toml;

import at.yawk.toml.test.TomlExpectedDocumentValidator;
import at.yawk.toml.test.TomlTestCase;
import io.micronaut.jackson.core.tree.JsonNodeTreeCodec;
import io.micronaut.json.JsonStreamConfig;
import io.micronaut.json.tree.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TomlTestSuiteTest {
    private static final TomlExpectedDocumentValidator VALIDATOR = new MicronautJsonNodeValidator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("at.yawk.toml.test.TomlTestSuite#validToml100")
    void validToml100(TomlTestCase testCase) throws IOException {
        String toml = tomlString(testCase);
        JsonNode actual = Parser.parse(toml, true);

        VALIDATOR.validate(testCase, unwrapObject(actual));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("at.yawk.toml.test.TomlTestSuite#invalidToml100")
    void invalidToml100(TomlTestCase testCase) {
        Assertions.assertThrows(IOException.class, () -> Parser.parse(tomlString(testCase), true), testCase::id);
    }

    private static String tomlString(TomlTestCase testCase) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(testCase.tomlBytes()))
            .toString();
    }

    private static Map<String, ?> unwrapObject(JsonNode node) {
        Assertions.assertTrue(node.isObject(), "TOML document root must be an object");
        Map<String, Object> unwrapped = new LinkedHashMap<>();
        node.entries().forEach(entry -> unwrapped.put(entry.getKey(), unwrap(entry.getValue())));
        return unwrapped;
    }

    private static Object unwrap(JsonNode node) {
        if (node.isNumber()) {
            return node.getNumberValue();
        }
        if (node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.getBooleanValue();
        }
        if (node.isArray()) {
            List<Object> unwrapped = new ArrayList<>();
            node.values().forEach(value -> unwrapped.add(unwrap(value)));
            return unwrapped;
        }
        if (node.isObject()) {
            return unwrapObject(node);
        }
        return node.getStringValue();
    }

    private static final class MicronautJsonNodeValidator extends TomlExpectedDocumentValidator {
        @Override
        protected Map<String, ?> parseExpectedJson(String expectedJson) {
            try {
                return unwrapObject(JsonNodeTreeCodec.getInstance()
                    .withConfig(JsonStreamConfig.DEFAULT.withUseBigDecimalForFloats(true))
                    .readTree(JsonFactory.builder()
                        .configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS, true)
                        .build()
                        .createParser(expectedJson)));
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid expected JSON", e);
            }
        }

        @Override
        protected void validateOffsetDateTime(String path, String expected, Object actual) {
            if (actual instanceof String actualString) {
                Assertions.assertEquals(OffsetDateTime.parse(normalizeDateTime(expected)), OffsetDateTime.parse(actualString), path);
            } else {
                super.validateOffsetDateTime(path, expected, actual);
            }
        }

        @Override
        protected void validateLocalDateTime(String path, String expected, Object actual) {
            if (actual instanceof String actualString) {
                Assertions.assertEquals(LocalDateTime.parse(normalizeDateTime(expected)), LocalDateTime.parse(actualString), path);
            } else {
                super.validateLocalDateTime(path, expected, actual);
            }
        }

        @Override
        protected void validateLocalDate(String path, String expected, Object actual) {
            if (actual instanceof String actualString) {
                Assertions.assertEquals(LocalDate.parse(expected), LocalDate.parse(actualString), path);
            } else {
                super.validateLocalDate(path, expected, actual);
            }
        }

        @Override
        protected void validateLocalTime(String path, String expected, Object actual) {
            if (actual instanceof String actualString) {
                Assertions.assertEquals(LocalTime.parse(expected), LocalTime.parse(actualString), path);
            } else {
                super.validateLocalTime(path, expected, actual);
            }
        }

        private static String normalizeDateTime(String value) {
            StringBuilder normalized = null;
            if (value.length() > 10 && value.charAt(10) != 'T') {
                normalized = new StringBuilder(value);
                normalized.setCharAt(10, 'T');
            }
            if (value.endsWith("z")) {
                if (normalized == null) {
                    normalized = new StringBuilder(value);
                }
                normalized.setCharAt(value.length() - 1, 'Z');
            }
            return normalized == null ? value : normalized.toString();
        }
    }
}
