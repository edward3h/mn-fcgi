package org.ethelred.cgi;

import java.util.concurrent.TimeUnit;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-12
 */
public interface CgiServer
{
    void init();

    void start(CgiHandler handler);

    void shutdown();

    boolean isSingleRequest();

    void waitForCompletion(long timeout, TimeUnit unit);
}
