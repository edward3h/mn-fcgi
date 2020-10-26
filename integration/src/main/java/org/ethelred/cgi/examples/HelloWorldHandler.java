package org.ethelred.cgi.examples;

import org.ethelred.cgi.CgiHandler;
import org.ethelred.cgi.CgiRequest;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-15
 */
public class HelloWorldHandler implements CgiHandler
{
    private final AtomicInteger counter = new AtomicInteger();
    @Override
    public void handleRequest(CgiRequest request)
    {
        try(PrintWriter w = new PrintWriter(new OutputStreamWriter(request.getOutput()))) {
            w.println("Content-type:text/plain");
            w.println();
            w.println("Hello, world!");
            w.println("Requests handled = " + counter.incrementAndGet());
        }

    }
}
