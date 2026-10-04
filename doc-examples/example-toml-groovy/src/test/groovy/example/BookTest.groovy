package example

import io.micronaut.serde.ObjectMapper
import io.micronaut.serde.toml.TomlObjectMapper
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Named
import spock.lang.Specification

@MicronautTest
class BookTest extends Specification {

    @Inject
    @Named(TomlObjectMapper.NAME)
    ObjectMapper tomlMapper

    void "test write read book"() {
        when:
        String result = tomlMapper.writeValueAsString(new Book("The Stand", 50))
        Book book = tomlMapper.readValue(result, Book)

        then:
        book.title == "The Stand"
        book.pages == 50
    }
}
