package org.ethelred.cgi.examples;

import org.ethelred.cgi.CgiServer;
import org.ethelred.cgi.graal.LibFCGIServer;

import java.util.concurrent.TimeUnit;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-15
 */
public class HelloWorldFcgi
{
    public static void main(String[] args)
    {
        CgiServer server = new LibFCGIServer();
        server.init();
        server.start(new HelloWorldHandler());
        server.waitForCompletion(5, TimeUnit.SECONDS);
    }
}
