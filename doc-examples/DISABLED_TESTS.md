# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples under `doc-examples/example-toml-python`
that are present but disabled, or that deviate from the Java example because the direct port does
not compile or does not behave like the Java example yet. It is the bug-fixing task list for
the Python compiler (`micronaut-inject-python` / `micronaut-context-python`); every row references a
`TODO(python)` comment in the sources or a workaround described below.

The Python examples are compiled by every build and their tests run with
`./gradlew pythonCheck -Ppython-ci` (the "Python CI" GitHub workflow).

## Reconciliation

- Last generated active `@Disabled` count: 0.
- Last generated command: `rg -n "@Disabled\(" doc-examples/example-toml-python/src`.
- Last full-suite command: `./gradlew :micronaut-doc-examples:micronaut-example-toml-python:test -Ppython-ci`.
- Last full-suite result: build successful, 1 test executed, 0 skipped, 0 failures.

## Migration Rules

- Do not define local copies of Micronaut annotation helpers or custom annotation shims in docs snippets.
  Standard Micronaut and Micronaut Serialization annotations are imported from their Java package
  (`micronaut.serde.annotation`, `jakarta.inject`, ...).
- The application code of the `source="main"` snippets lives in `src/main/python`, the tests in `src/test/python`;
  both roots are merged into one directory compiled by `compileTestPython` (see the `TODO(python)` in
  `doc-examples/example-toml-python/build.gradle`): compiling them separately yields two GraalPy VFS roots whose
  generated shim modules shadow each other, and the imports of a source file are only resolved within its own root.
  Classes of the other root are imported with absolute imports (`from example.Book import Book`).
- `Book` is a `@Serdeable` `@dataclass` (no Java-style getters); the attribute names are used verbatim as the TOML keys.
- The TOML mapper is injected with the `@Named(TomlObjectMapper.NAME)` qualifier in `Annotated[...]`
  (`toml_mapper: Annotated[ObjectMapper, Named(TomlObjectMapper.NAME)]`); the Java constant works as the annotation member.
- The Python test injects the mapper as a class attribute (`Annotated[ObjectMapper, Inject, Named(TomlObjectMapper.NAME)]`)
  instead of the test-method parameter of the Java example, and the examples' own Python class `Book` works as the
  runtime type argument of `readValue(result, Book)`.
- Java classes are imported (`from micronaut.serde import ObjectMapper`, `from micronaut.serde.toml import TomlObjectMapper`);
  no `java.type(...)` alias is needed by these examples.

## Active `@Disabled` Tests

None.

## Commented Unsupported Snippet Ports

None.

## Workarounds Kept In Snippets

None.

## Intentionally Unsupported Snippet Targets

None.

## java.type usages

None.
