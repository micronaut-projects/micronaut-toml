package example

import io.micronaut.serde.ObjectMapper
import io.micronaut.serde.toml.TomlObjectMapper
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Named
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest
class BookTest {

    @Test
    fun testWriteReadBook(@Named(TomlObjectMapper.NAME) tomlMapper: ObjectMapper) {
        val result = tomlMapper.writeValueAsString(Book("The Stand", 50))

        val book = tomlMapper.readValue(result, Book::class.java)!!
        assertEquals("The Stand", book.title)
        assertEquals(50, book.pages)
    }
}
