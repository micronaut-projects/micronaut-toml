package io.micronaut.toml.env

import io.micronaut.context.ApplicationContext
import spock.lang.Specification


class TomlPropertySourceLoaderSpec extends Specification {

    void "test TOML file is automatically loaded via environment"() {
        when:
        ApplicationContext ctx = ApplicationContext.builder()
                .environments("mytest1")
                .start()

        then:
        ctx.environment.get("data-source.username", String).get() == "sa"
        ctx.environment.get("data-source.password", String).get() == "secret"
        ctx.environment.get("data-source.jmx-export", Boolean).get()
        !ctx.environment.get("hibernate.cache-queries", Boolean).get()

        cleanup:
        ctx.close()
    }
}
