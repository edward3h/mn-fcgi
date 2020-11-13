package org.ethelred.hello

import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
class HelloMnSpec extends Specification {
    @Shared
    @Client("http://localhost:8088/integration-mn")
    @Inject
    RxHttpClient httpClient;

    def "test say hello"() {
        when:
        String result = httpClient.toBlocking().retrieve("/hi");

        then:
        result == "Hello world!"
    }
}
