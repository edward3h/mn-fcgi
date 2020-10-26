package org.ethelred.cgi.graal;

import org.ethelred.cgi.CgiServer;
import org.ethelred.cgi.graal.libfcgi.LibFCGI;
import org.ethelred.cgi.plain.PlainCgiServer;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-22
 */
public class CgiServerFactory
{
    private CgiServer instance;

    public synchronized CgiServer getInstance() {
        if (instance == null) {
            if (LibFCGI.FCGX_IsCGI() > 0) {
                instance = new PlainCgiServer();
            } else {
                instance = new LibFCGIServer();
            }
        }
        return instance;
    }
}
