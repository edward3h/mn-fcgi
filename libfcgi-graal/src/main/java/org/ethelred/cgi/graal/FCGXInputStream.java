package org.ethelred.cgi.graal;


import org.ethelred.cgi.graal.libfcgi.FCGX_Stream;
import org.ethelred.cgi.graal.libfcgi.LibFCGI;

import java.io.IOException;
import java.io.InputStream;

import static org.ethelred.cgi.graal.libfcgi.LibFCGI.FCGX_GetChar;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-02
 */
public class FCGXInputStream extends InputStream
{
    private final FCGX_Stream in;

    public FCGXInputStream(FCGX_Stream in)
    {
        this.in = in;
    }

    @Override
    public int read() throws IOException
    {
        return FCGX_GetChar(in);
    }
}
