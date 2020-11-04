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
    void init(Callback callback);

    void start(CgiHandler handler);

    void shutdown();

    boolean isSingleRequest();

    void waitForCompletion(long timeout, TimeUnit unit);

    boolean isRunning();

    interface Callback {
        void onCompleted();

        static Callback ignore() {
            return () -> {
                // ignore
            };
        }
    }
}
