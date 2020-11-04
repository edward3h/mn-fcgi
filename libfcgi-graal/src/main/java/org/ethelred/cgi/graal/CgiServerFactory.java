package org.ethelred.cgi.graal;

import org.ethelred.cgi.CgiServer;
import org.ethelred.cgi.graal.libfcgi.LibFCGI;
import org.ethelred.cgi.plain.PlainCgiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-22
 */
public class CgiServerFactory implements Supplier<CgiServer>
{
    protected static final Logger LOG = LoggerFactory.getLogger(CgiServerFactory.class);
    private CgiServer instance;

    public synchronized CgiServer get() {
        if (instance == null) {
            try
            {
                if (LibFCGI.FCGX_IsCGI() > 0)
                {
                    instance = new PlainCgiServer();
                } else
                {
                    instance = new LibFCGIServer();
                }
            } catch (Throwable e) {
                LOG.warn("Error evaluating CGI type, will default to plain CGI", e);
                instance = new PlainCgiServer();
            }
        }
        return instance;
    }
}
