package example;

import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.toml.TomlObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class BookTest {

    @Test
    void testWriteReadBook(@Named(TomlObjectMapper.NAME) ObjectMapper tomlMapper) throws IOException {
        String result = tomlMapper.writeValueAsString(new Book("The Stand", 50));

        Book book = tomlMapper.readValue(result, Book.class);
        assertEquals("The Stand", book.getTitle());
        assertEquals(50, book.getPages());
    }
}
