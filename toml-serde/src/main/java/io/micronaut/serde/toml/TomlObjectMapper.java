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
package io.micronaut.serde.toml;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonStreamConfig;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.serde.Deserializer;
import io.micronaut.serde.Encoder;
import io.micronaut.serde.LimitingStream;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.SerdeRegistry;
import io.micronaut.serde.Serializer;
import io.micronaut.serde.config.SerdeConfiguration;
import io.micronaut.serde.config.naming.PropertyNamingStrategy;
import io.micronaut.serde.support.util.JsonNodeDecoder;
import io.micronaut.serde.support.util.JsonNodeEncoder;
import io.micronaut.serde.toml.encodestyle.InlineRootEncoder;
import io.micronaut.serde.toml.encodestyle.TableRootEncoder;
import io.micronaut.serde.toml.support.MicronautTomlParserAdapter;
import io.micronaut.serde.toml.support.SerdeTomlConfiguration;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

/**
 * A TOML-backed {@link ObjectMapper}.
 *
 * <p>The serialization output style is controlled by
 * {@link SerdeTomlConfiguration.WriteFeatures#getWriteLayout()}
 * ({@code micronaut.serde.toml.write-features.write-layout}):
 * <ul>
 *   <li>{@link SerdeTomlConfiguration.WriteLayout#TABLE TABLE} (default) as
 *       {@code [table]} headers
 *   <li>{@link SerdeTomlConfiguration.WriteLayout#INLINE INLINE} — as inline
 *       tables, e.g. {@code key = {a = 1, b = 2}}.</li>
 * </ul>
 *
 * <p>Reads are guarded by {@link SerdeTomlConfiguration.ReadConstraints}
 * ({@code micronaut.serde.toml.read-constraints.*}) — maximum document size, string-value length and
 * number-token length — together with the serde nesting-depth limit.
 *
 * @author Mousrij Hamza
 * @since 3.0.1
 */
@Singleton
@Named(TomlObjectMapper.NAME)
@Internal
@SuppressWarnings({"rawtypes", "unchecked"})
public final class TomlObjectMapper implements ObjectMapper {

    /**
     * The qualifier name of the TOML {@link ObjectMapper} bean.
     */
    public static final String NAME = "toml";

    private final SerdeRegistry registry;
    @Nullable
    private final SerdeConfiguration serdeConfiguration;
    private final SerdeTomlConfiguration tomlConfiguration;
    private final MicronautTomlParserAdapter parserAdapter;

    /**
     * Creates a TOML-backed {@link ObjectMapper}.
     *
     * @param registry           The serde registry
     * @param serdeConfiguration The serde configuration
     * @param tomlConfiguration  The TOML-specific configuration
     */
    public TomlObjectMapper(SerdeRegistry registry,
                            SerdeConfiguration serdeConfiguration,
                            SerdeTomlConfiguration tomlConfiguration) {
        this(
            registry,
            serdeConfiguration,
            tomlConfiguration,
            new MicronautTomlParserAdapter(serdeConfiguration, tomlConfiguration)
        );
    }

    private TomlObjectMapper(SerdeRegistry registry,
                             @Nullable SerdeConfiguration serdeConfiguration,
                             SerdeTomlConfiguration tomlConfiguration,
                             MicronautTomlParserAdapter parserAdapter) {
        this.registry = registry;
        this.serdeConfiguration = serdeConfiguration;
        this.tomlConfiguration = tomlConfiguration;
        this.parserAdapter = parserAdapter;
    }

    /**
     * Returns the {@link SerdeRegistry} used by this object mapper, if possible.
     *
     * @return The serde registry
     */
    @Override
    public @NonNull SerdeRegistry getSerdeRegistry() {
        return registry;
    }

    /**
     * Parse and map Toml from the given stream.
     *
     * @param inputStream The input data.
     * @param type The type to deserialize to.
     * @param <T> Type variable of the return type.
     * @return The deserialized object.
     * @throws IOException IOException
     */
    @Override
    public <T> @Nullable T readValue(@NonNull InputStream inputStream, @NonNull Argument<T> type) throws IOException {
        JsonNode tree = parserAdapter.parse(inputStream);
        Deserializer.DecoderContext decoderContext = registry.newDecoderContext(null);
        Deserializer<? extends T> deserializer = decoderContext.findDeserializer(type).createSpecific(decoderContext, type);
        return deserializer.deserializeNullable(JsonNodeDecoder.create(tree, limits()), decoderContext, type);
    }

    /**
     * Parse and map Toml from the given byte array.
     *
     * @param byteArray The input data.
     * @param type The type to deserialize to.
     * @param <T> Type variable of the return type.
     * @return The deserialized object.
     * @throws IOException IOException
     */
    @Override
    public <T> @Nullable T readValue(byte @NonNull [] byteArray, @NonNull Argument<T> type) throws IOException {
        return readValue(new ByteArrayInputStream(byteArray), type);
    }

    /**
     * Transform a {@link JsonNode} to a value of the given type.
     *
     * @param tree The input json data.
     * @param type The type to deserialize.
     * @param <T> Type variable of the return type.
     * @return The deserialized value.
     * @throws IOException IOException
     */
    @Override
    public <T> @Nullable T readValueFromTree(@NonNull JsonNode tree, @NonNull Argument<T> type) throws IOException {
        Deserializer.DecoderContext decoderContext = registry.newDecoderContext(null);
        Deserializer<? extends T> deserializer = decoderContext.findDeserializer(type).createSpecific(decoderContext, type);
        return deserializer.deserializeNullable(JsonNodeDecoder.create(tree, limits()), decoderContext, type);
    }

    /**
     * Transform an object value to a json tree.
     *
     * @param value The object value to transform.
     * @return The json representation.
     * @throws IOException If there are any mapping exceptions (e.g. illegal values).
     */
    @Override
    public @NonNull JsonNode writeValueToTree(@Nullable Object value) throws IOException {
        if (value == null) {
            return JsonNode.nullNode();
        }
        JsonNodeEncoder encoder = JsonNodeEncoder.create(limits());
        serialize(encoder, value);
        return encoder.getCompletedValue();
    }

    /**
     * Transform an object value to a json tree.
     *
     * @param type The object type
     * @param value The object value to transform.
     * @param <T> The type variable of the type.
     * @return The json representation.
     * @throws IOException If there are any mapping exceptions (e.g. illegal values).
     */
    @Override
    public @NonNull <T> JsonNode writeValueToTree(@NonNull Argument<T> type, @Nullable T value) throws IOException {
        if (value == null) {
            return JsonNode.nullNode();
        }
        JsonNodeEncoder encoder = JsonNodeEncoder.create(limits());
        serialize(encoder, value, type);
        return encoder.getCompletedValue();
    }

    /**
     * Write an object as Toml using JsonNode entities.
     *
     * @param outputStream The stream to write to.
     * @param object The object to serialize.
     * @throws IOException IOException
     */
    @Override
    public void writeValue(@NonNull OutputStream outputStream, @Nullable Object object) throws IOException {
        if (object == null) {
            return;
        }
        writeToml(outputStream, writeValueAsTomlTree(object));
    }

    /**
     * Write an object as Toml using JsonNode entities.
     *
     * @param outputStream The stream to write to.
     * @param type The object type
     * @param object The object to serialize.
     * @param <T> The generic type
     * @throws IOException IOException
     */
    @Override
    public <T> void writeValue(@NonNull OutputStream outputStream, @NonNull Argument<T> type, @Nullable T object) throws IOException {
        if (object == null) {
            return;
        }
        writeToml(outputStream, writeValueAsTomlTree(type, object));
    }

    /**
     * Write an object as Toml using JsonNode entities.
     *
     * @param object The object to serialize.
     * @return The serialized encoded json.
     * @throws IOException IOException
     */
    @Override
    public byte @NonNull [] writeValueAsBytes(@Nullable Object object) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeValue(output, object);
        return output.toByteArray();
    }

    /**
     * Write an object as Toml using JsonNode entities.
     *
     * @param type The object type
     * @param object The object to serialize.
     * @param <T> The generic type
     * @return The serialized encoded json.
     * @throws IOException IOException
     */
    @Override
    public <T> byte @NonNull [] writeValueAsBytes(@NonNull Argument<T> type, @Nullable T object) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeValue(output, type, object);
        return output.toByteArray();
    }

    /**
     * @return The configured stream config.
     */
    @Override
    public @NonNull JsonStreamConfig getStreamConfig() {
        return JsonStreamConfig.DEFAULT;
    }

    private LimitingStream.@NonNull RemainingLimits limits() {
        return serdeConfiguration == null ? LimitingStream.DEFAULT_LIMITS : LimitingStream.limitsFromConfiguration(serdeConfiguration);
    }

    private JsonNode writeValueAsTomlTree(@NonNull Object value) throws IOException {
        return writeValueAsTomlTree((Argument) Argument.of(value.getClass()), value);
    }

    private <T> JsonNode writeValueAsTomlTree(@NonNull Argument<T> type, @NonNull T value) throws IOException {
        JsonNodeEncoder encoder = JsonNodeEncoder.create(limits());
        serialize(encoder, value);
        return omitNullObjectProperties(encoder.getCompletedValue());
    }

    private void writeToml(@NonNull OutputStream outputStream, @NonNull JsonNode value) throws IOException {
        StringBuilder builder = new StringBuilder();
        switch (tomlConfiguration.getWriteLayout()) {
            case TABLE -> TableRootEncoder.appendTableDocument(builder, value);
            case INLINE -> InlineRootEncoder.appendInlineDocument(builder, value);
        }
        outputStream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private static JsonNode omitNullObjectProperties(JsonNode value) {
        if (value.isObject()) {
            Map<String, JsonNode> values = new LinkedHashMap<>();
            boolean changed = false;
            for (Map.Entry<String, JsonNode> entry : value.entries()) {
                JsonNode entryValue = entry.getValue();
                if (entryValue.isNull()) {
                    changed = true;
                    continue;
                }
                JsonNode normalizedValue = omitNullObjectProperties(entryValue);
                values.put(entry.getKey(), normalizedValue);
                changed |= normalizedValue != entryValue;
            }
            return changed ? JsonNode.createObjectNode(values) : value;
        }
        if (value.isArray()) {
            List<JsonNode> values = new ArrayList<>(value.size());
            boolean changed = false;
            for (JsonNode entryValue : value.values()) {
                JsonNode normalizedValue = omitNullObjectProperties(entryValue);
                values.add(normalizedValue);
                changed |= normalizedValue != entryValue;
            }
            return changed ? JsonNode.createArrayNode(values) : value;
        }
        return value;
    }

    private void serialize(@NonNull Encoder encoder, @NonNull Object value) throws IOException {
        serialize(encoder, value, (Argument) Argument.of(value.getClass()));
    }

    private <T> void serialize(@NonNull Encoder encoder, @NonNull T value, @NonNull Argument<T> type) throws IOException {
        Serializer.EncoderContext encoderContext = registry.newEncoderContext(null);
        Serializer serializer = encoderContext.findSerializer(type).createSpecific(encoderContext, type);
        serializer.serialize(encoder, encoderContext, type, value);
    }
}
