package io.micronaut.toml.env

import io.micronaut.context.ApplicationContext
import spock.lang.Specification
import io.micronaut.context.env.PropertySource


class TomlPropertySourceLoaderSpec extends Specification {

    void "test toml property source loader"() {
        given:
        def loader = new TomlPropertySourceLoader()

        def testToml = '''\
[dataSource]
jmxExport = true
username = "sa"
password = "test"
'''

        def appToml = '''\
[hibernate]
cache.queries = false

[dataSource]
pooled = true
driverClassName = "org.h2.Driver"
username = "sa"
password = "test"
something = [1, 2]
'''

        def map1 = loader.read("application-test", new ByteArrayInputStream(testToml.bytes))
        def map2 = loader.read("application", new ByteArrayInputStream(appToml.bytes))

        def ps1 = PropertySource.of("application-test", map1)
        def ps2 = PropertySource.of("application", map2)

        ApplicationContext ctx = ApplicationContext.builder()
                .environments("test")
                .start()

        ctx.environment.addPropertySource(ps1)
        ctx.environment.addPropertySource(ps2)

        when:
        ctx.environment.refresh()

        then:
        !ctx.environment.get("hibernate.cache.queries", Boolean).get()
        ctx.environment.get("data-source.pooled", Boolean).get()
        ctx.environment.get("data-source.password", String).get() == 'test'
        ctx.environment.get("data-source.jmx-export", boolean).get()
        ctx.environment.get("data-source.something", List).get() == [1,2]

        cleanup:
        ctx.close()
    }
}
