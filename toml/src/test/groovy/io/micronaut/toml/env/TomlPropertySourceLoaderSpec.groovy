package io.micronaut.toml.env

import io.micronaut.context.ApplicationContextConfiguration
import io.micronaut.context.env.DefaultEnvironment
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySourceLoader
import io.micronaut.core.io.service.ServiceDefinition
import io.micronaut.core.io.service.SoftServiceLoader
import io.micronaut.jackson.core.env.JsonPropertySourceLoader
import spock.lang.Specification

class TomlPropertySourceLoaderSpec extends Specification {
    void "test toml property source loader"() {
        given:
        def serviceDefinition = Mock(ServiceDefinition)
        serviceDefinition.isPresent() >> true
        serviceDefinition.load() >> new JsonPropertySourceLoader()

        Environment env = new DefaultEnvironment(new ApplicationContextConfiguration() {
            @Override
            List<String> getEnvironments() {
                return ["test"]
            }
        }) {
            @Override
            protected SoftServiceLoader<PropertySourceLoader> readPropertySourceLoaders() {
                GroovyClassLoader gcl = new GroovyClassLoader()
                gcl.addURL(JsonPropertySourceLoader.getResource("/META-INF/services/io.micronaut.context.env.PropertySourceLoader"))
                return new SoftServiceLoader<PropertySourceLoader>(PropertySourceLoader, gcl)
            }

            @Override
            Optional<InputStream> getResourceAsStream(String path) {
                if(path.endsWith('-test.toml')) {
                    return Optional.of(new ByteArrayInputStream('''\
[dataSource]
jmxExport = true
username = "sa"
password = "test"
'''.bytes))
                }
                else if(path.endsWith("application.toml")) {
                    return Optional.of(new ByteArrayInputStream('''\
[hibernate]
cache.queries = false

[dataSource]
pooled = true
driverClassName = "org.h2.Driver"
username = "sa"
password = "test"
something = [1, 2]
'''.bytes))
                }
                return Optional.empty()
            }
        }


        when:
        env.start()

        then:
        !env.get("hibernate.cache.queries", Boolean).get()
        env.get("data-source.pooled", Boolean).get()
        env.get("data-source.password", String).get() == 'test'
        env.get("data-source.jmx-export", boolean).get()
        env.get("data-source.something", List).get() == [1,2]



    }
}
