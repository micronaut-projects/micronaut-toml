package example

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class Book {
    final String title
    final int pages

    Book(String title, int pages) {
        this.title = title
        this.pages = pages
    }
}
