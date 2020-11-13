package org.ethelred.cgi.examples

import com.agorapulse.gru.Client
import com.agorapulse.gru.Gru
import com.agorapulse.gru.http.Http
import com.agorapulse.gru.minions.AbstractContentMinion
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

/**
 * TODO
 *
 * @author eharman* @since 2020-10-25
 */
class CgiSpec extends Specification {
    @Rule Gru<Http> gru = Gru.equip(Http.steal(this))
            .prepare('http://localhost:8088/integration/')

    @Unroll
    def "get request #page to contain #expectedText"(page, expectedText) {
        expect:
        gru.test {
            get page
            expect {
                def content = inline(expectedText)
                command(ContainsMinion.class, m -> m.setResponseContent(content))
            }
        }

        where:
        page | expectedText
        "bin/hello_world.cgi" | "Hello, world!"
        "native/hello_world.fcgi" | "Hello, world!"
    }
}

class ContainsMinion extends AbstractContentMinion<Client> {

    final int index = TEXT_MINION_INDEX

    ContainsMinion() {
        super(Client)
    }

    protected void similar(String actual, String expected) throws AssertionError {
        assert actual.contains(expected)
    }
}
