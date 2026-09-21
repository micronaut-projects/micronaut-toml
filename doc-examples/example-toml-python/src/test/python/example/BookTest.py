from typing import Annotated

from jakarta.inject import Inject, Named
from micronaut.serde import ObjectMapper
from micronaut.serde.toml import TomlObjectMapper
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Book import Book


@MicronautTest
class BookTest:

    toml_mapper: Annotated[ObjectMapper, Inject, Named(TomlObjectMapper.NAME)]

    @Test
    def test_write_read_book(self) -> None:
        result = self.toml_mapper.writeValueAsString(Book("The Stand", 50))

        book = self.toml_mapper.readValue(result, Book)
        assert book.title == "The Stand"
        assert book.pages == 50
