package org.ethelred.hello

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Specification
import io.micronaut.test.support.server.TestEmbeddedServer

import javax.inject.Inject

/**
 * TODO
 *
 * @author eharman* @since 2020-11-12
 */
@MicronautTest
@Property(name = TestEmbeddedServer.PROPERTY, value = "http://localhost:8088/integration-mn/")
class HelloNameSpec extends Specification {
    @Inject
    @Shared
    HelloNameClient client;

    def "test hello name"() {
        when:
        String result = client.hello("Fred").blockingGet();

        then:
        result == "Hello Fred"
    }
}
