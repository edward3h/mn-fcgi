package org.ethelred.html_writer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-12
 */
public interface HtmlWriter
{
    void accept(Writer writer) throws IOException;

    default String buildString() {
        StringWriter buf = new StringWriter();
        try
        {
            accept(buf);
        }
        catch (IOException e)
        {
            // StringWriter does not throw exceptions
        }
        return buf.toString();
    }
}
