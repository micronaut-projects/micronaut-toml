package io.micronaut.toml.env

import io.micronaut.context.ApplicationContext
import spock.lang.Specification


class TomlPropertySourceLoaderSpec extends Specification {

    void "test TOML file is automatically loaded via environment"() {
        when:
        ApplicationContext ctx = ApplicationContext.builder()
                .environments("test")
                .start()

        then:
        !ctx.environment.get("hibernate.cache.queries", Boolean).get()
        ctx.environment.get("data-source.pooled", Boolean).get()
        ctx.environment.get("data-source.password", String).get() == 'test'
        ctx.environment.get("data-source.jmx-export", Boolean).get()
        ctx.environment.get("data-source.something", List).get() == [1, 2]

        cleanup:
        ctx.close()
    }
}
