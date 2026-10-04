package example

import io.micronaut.serde.ObjectMapper
import io.micronaut.serde.toml.TomlObjectMapper
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
class TomlService(@Named(TomlObjectMapper.NAME) private val tomlMapper: ObjectMapper) {

    fun tomlMapper(): ObjectMapper = tomlMapper
}
