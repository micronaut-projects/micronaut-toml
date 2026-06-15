package example;

import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.toml.TomlObjectMapper;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
final class TomlService {
    private final ObjectMapper tomlMapper;

    TomlService(@Named(TomlObjectMapper.NAME) ObjectMapper tomlMapper) {
        this.tomlMapper = tomlMapper;
    }

    ObjectMapper tomlMapper() {
        return tomlMapper;
    }
}
