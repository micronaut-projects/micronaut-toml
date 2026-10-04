from typing import Annotated

from jakarta.inject import Named, Singleton
from micronaut.serde import ObjectMapper
from micronaut.serde.toml import TomlObjectMapper


@Singleton
class TomlService:

    def __init__(self, toml_mapper: Annotated[ObjectMapper, Named(TomlObjectMapper.NAME)]):
        self._toml_mapper = toml_mapper

    def toml_mapper(self) -> ObjectMapper:
        return self._toml_mapper
