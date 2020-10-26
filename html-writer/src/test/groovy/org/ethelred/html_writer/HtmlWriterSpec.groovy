package org.ethelred.html_writer

import spock.lang.Specification

/**
 * TODO
 *
 * @author eharman* @since 2020-10-13
 */
class HtmlWriterSpec extends Specification {
    def "simple snippet example"() {
        given:
        def myView = new Tags() {
            def generate() {
                div(id("outer").className("bold"),
                        a(href("/page"), t("a link")),
                        span(className("bold"), t("some text"))
                ).buildString()
            }
        }

        when:
        def s = myView.generate()

        then:
        s == $/<div id="outer" class="bold"><a href="/page">a link</a><span class="bold">some text</span></div>/$
    }

    def "multiple classnames"() {
        given:
        def myView = new Tags() {
            def generate() {
                span(className("one").className("two"), t("foo")).buildString()
            }
        }

        when:
        def s = myView.generate()

        then:
        s == "<span class=\"one two\">foo</span>"
    }
}
