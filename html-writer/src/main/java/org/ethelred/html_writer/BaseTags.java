package org.ethelred.html_writer;

import org.ethelred.util.function.CheckedConsumer;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-12
 */
public class BaseTags
{
    protected BaseTags() {}

    public HtmlWriter t(Object text) {
        return buf -> buf.append(Objects.toString(text));
    }

    public HtmlWriter empty() {
        return buf -> {};
    }

    public HtmlWriter tag(
            String name,
            BaseAttributes<?> attributes,
            HtmlWriter... inner
    ) {
        return buf -> {
            _openTag(name, attributes, buf);
            for (HtmlWriter i : inner) {
                i.accept(buf);
            }
            _closeTag(name, buf);
        };
    }

    public HtmlWriter tag(
            String name,
            BaseAttributes<?> attributes,
            Iterable<HtmlWriter> inner
    ) {
        return buf -> {
            _openTag(name, attributes, buf);
            for (HtmlWriter i : inner) {
                i.accept(buf);
            }
            _closeTag(name, buf);
        };
    }

    public HtmlWriter tag(
            String name,
            BaseAttributes<?> attributes,
            Stream<HtmlWriter> inner
    ) {
        return buf -> {
            _openTag(name, attributes, buf);
            inner.forEach(CheckedConsumer.unchecked(i -> i.accept(buf)));
            _closeTag(name, buf);
        };
    }

    private void _closeTag(String name, Writer buf) throws IOException
    {
        buf.append("</").append(name).append(">");
    }

    private void _openTag(String name, BaseAttributes<?> attributes, Writer buf) throws IOException
    {
        buf.append("<").append(name);
        attributes.write(buf);
        buf.append(">");
    }

    public HtmlWriter iff(boolean condition, HtmlWriter truePath, HtmlWriter falsePath) {
        return condition ? truePath : falsePath;
    }

    public HtmlWriter ifNotNull(Object obj, HtmlWriter inner) {
        return iff(obj != null, inner, empty());
    }
}
