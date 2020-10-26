package org.ethelred.cgi.examples;

import org.ethelred.cgi.CgiServer;
import org.ethelred.cgi.plain.PlainCgiServer;

import java.util.concurrent.TimeUnit;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-15
 */
public class HelloWorldCgi
{
    public static void main(String[] args)
    {
        CgiServer server = new PlainCgiServer();
        server.init();
        server.start(new HelloWorldHandler());
        server.waitForCompletion(5, TimeUnit.SECONDS);
    }
}
