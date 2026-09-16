package example

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class Book(val title: String, val pages: Int)
