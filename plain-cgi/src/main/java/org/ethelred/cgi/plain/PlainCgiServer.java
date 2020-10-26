package org.ethelred.cgi.plain;

import org.ethelred.cgi.CgiHandler;
import org.ethelred.cgi.CgiServer;

import java.util.concurrent.TimeUnit;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-12
 */
public class PlainCgiServer implements CgiServer
{
    @Override
    public void init()
    {
        // no-op
    }

    @Override
    public void start(CgiHandler handler)
    {
        handler.handleRequest(new SystemCgiRequest());
    }

    @Override
    public void shutdown()
    {
        // no-op
    }

    @Override
    public boolean isSingleRequest()
    {
        return true;
    }

    @Override
    public void waitForCompletion(long timeout, TimeUnit unit)
    {
        // no-op
    }
}
