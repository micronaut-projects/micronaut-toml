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
package io.micronaut.serde.toml.support;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;
import io.micronaut.serde.config.SerdeConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * TOML-specific configuration.
 *
 * @since 3.0.1
 */
@BootstrapContextCompatible
@Internal
@ConfigurationProperties(SerdeTomlConfiguration.PREFIX)
public final class SerdeTomlConfiguration {
    static final String PREFIX = SerdeConfiguration.PREFIX + ".toml";

    private ReadConstraints readConstraints = new ReadConstraints();
    private WriteFeatures writeFeatures = new WriteFeatures();

    /**
     * @return The TOML read constraints
     */
    public ReadConstraints getReadConstraints() {
        return readConstraints;
    }

    /**
     * @param readConstraints The TOML read constraints
     */
    public void setReadConstraints(ReadConstraints readConstraints) {
        this.readConstraints = readConstraints;
    }

    /**
     * @return The TOML write features
     */
    public WriteFeatures getWriteFeatures() {
        return writeFeatures;
    }

    /**
     * @param writeFeatures The TOML write features
     */
    public void setWriteFeatures(WriteFeatures writeFeatures) {
        this.writeFeatures = writeFeatures;
    }

    /**
     * @return The configured write layout
     */
    public WriteLayout getWriteLayout() {
        return writeFeatures.getWriteLayout();
    }

    /**
     * @return The maximum number token length, or {@code null} to use the default
     */
    public @Nullable Integer getMaxNumberLength() {
        return readConstraints.getMaxNumberLength();
    }

    /**
     * @return The maximum string value length, or {@code null} for no limit
     */
    public @Nullable Integer getMaxStringLength() {
        return readConstraints.getMaxStringLength();
    }

    /**
     * @return The maximum document size in bytes, or {@code null}/non-positive for no limit
     */
    public @Nullable Integer getMaxDocumentSize() {
        return readConstraints.getMaxDocumentSize();
    }

    /**
     * TOML writer layout.
     *
     * @since 3.0.1
     */
    public enum WriteLayout {
        TABLE,
        INLINE
    }

    /**
     * TOML read constraints.
     *
     * @since 3.0.1
     */
    @ConfigurationProperties("read-constraints")
    public static final class ReadConstraints {
        /**
         * Guarding against memory exhaustion before parsing a Toml Document, DOS attacks.
         * Configure a non-positive value to disable the limit.
         *
         */
        public static final int DEFAULT_MAX_DOCUMENT_SIZE = 1 * 1024 * 1024; // 1 MiB

        @Nullable
        private Integer maxNumberLength;
        @Nullable
        private Integer maxStringLength;
        @Nullable
        private Integer maxDocumentSize = DEFAULT_MAX_DOCUMENT_SIZE;

        /**
         * @return The maximum number token length, or {@code null} to use the default
         */
        public @Nullable Integer getMaxNumberLength() {
            return maxNumberLength;
        }

        /**
         * @param maxNumberLength The maximum number token length
         */
        public void setMaxNumberLength(@Nullable Integer maxNumberLength) {
            this.maxNumberLength = maxNumberLength;
        }

        /**
         * @return The maximum string value length, or {@code null} for no limit
         */
        public @Nullable Integer getMaxStringLength() {
            return maxStringLength;
        }

        /**
         * @param maxStringLength The maximum string value length
         */
        public void setMaxStringLength(@Nullable Integer maxStringLength) {
            this.maxStringLength = maxStringLength;
        }

        /**
         * @return The maximum document size in bytes, or {@code null}/non-positive for no limit
         */
        public @Nullable Integer getMaxDocumentSize() {
            return maxDocumentSize;
        }

        /**
         * @param maxDocumentSize The maximum document size in bytes ({@code null} or non-positive disables the limit)
         */
        public void setMaxDocumentSize(@Nullable Integer maxDocumentSize) {
            this.maxDocumentSize = maxDocumentSize;
        }
    }

    /**
     * Controls TOML serialization behavior.
     *
     * @since 3.0.1
     */
    @ConfigurationProperties("write-features")
    public static final class WriteFeatures {
        private WriteLayout writeLayout = WriteLayout.TABLE;

        /**
         * @return The configured write layout (defaults to {@link WriteLayout#TABLE})
         */
        public WriteLayout getWriteLayout() {
            return writeLayout;
        }

        /**
         * @param writeLayout The write layout
         */
        public void setWriteLayout(WriteLayout writeLayout) {
            this.writeLayout = Objects.requireNonNull(writeLayout, "writeLayout");
        }
    }
}
