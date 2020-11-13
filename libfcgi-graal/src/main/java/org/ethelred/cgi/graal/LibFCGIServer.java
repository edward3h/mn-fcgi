package org.ethelred.cgi.graal;

import org.ethelred.cgi.CgiHandler;
import org.ethelred.cgi.CgiServer;
import org.ethelred.cgi.graal.libfcgi.FCGX_Request;
import org.graalvm.nativeimage.StackValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.ethelred.cgi.graal.libfcgi.LibFCGI.FCGX_Accept_r;
import static org.ethelred.cgi.graal.libfcgi.LibFCGI.FCGX_Finish_r;
import static org.ethelred.cgi.graal.libfcgi.LibFCGI.FCGX_Init;
import static org.ethelred.cgi.graal.libfcgi.LibFCGI.FCGX_InitRequest;
import static org.ethelred.cgi.graal.libfcgi.LibFCGI.FCGX_ShutdownPending;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-12
 */
public class LibFCGIServer implements CgiServer
{
    private static final Logger log = LoggerFactory.getLogger(LibFCGIServer.class);

    private final ExecutorService executor = Executors.newWorkStealingPool();
    private final Lock acceptLock = new ReentrantLock();
    private final Semaphore newJob = new Semaphore(1);

    private enum State { CONSTRUCTED, INITIALIZED, RUNNING, FINISHED }

    private final AtomicReference<State> state = new AtomicReference<>(State.CONSTRUCTED);

    private boolean _checkTransition(State from, State to) {
        State actual = state.compareAndExchange(from, to);
        if (actual == from) {
            return true;
        }
        log.warn("Invalid state transition, expected {}, actual {}", from, actual);
        return false;
    }



    @Override
    public void init(Callback callback)
    {
        if (_checkTransition(State.CONSTRUCTED, State.INITIALIZED)) {
            FCGX_Init();
        }
    }

    @Override
    public void start(CgiHandler handler)
    {
        if (_checkTransition(State.INITIALIZED, State.RUNNING)) {
            while (state.get() == State.RUNNING) {
                try
                {
                    newJob.acquire();
                    executor.execute(new Worker(handler));
                } catch (InterruptedException ignore) {
                    // ignore
                } catch (Exception e) {
                    log.error(
                            "Unhandled worker exception", e
                    );
                }
            }
        }
    }

    @Override
    public void shutdown()
    {
        if(_checkTransition(State.RUNNING, State.FINISHED)) {
            executor.shutdown();
            FCGX_ShutdownPending();
        }

    }

    @Override
    public boolean isSingleRequest()
    {
        return false;
    }

    @Override
    public void waitForCompletion(long timeout, TimeUnit unit)
    {
        try
        {
            executor.awaitTermination(timeout, unit);
        }
        catch (InterruptedException ignore)
        {
            //ignore
        }
    }

    @Override
    public boolean isRunning()
    {
        return state.get() == State.RUNNING;
    }

    private class Worker implements Runnable {

        private final CgiHandler handler;

        public Worker(CgiHandler handler)
        {
            this.handler = handler;
        }

        @Override
        public void run()
        {
            FCGX_Request request = StackValue.get(FCGX_Request.class);
            FCGX_InitRequest(request, 0, 0);

            acceptLock.lock();
            try {
                FCGX_Accept_r(request);
            } finally
            {
                acceptLock.unlock();
                newJob.release();
            }

            try {
                handler.handleRequest(new LibFCGIRequest(request));
            } catch(Exception e)
            {
                log.error("Exception in request handler", e);
            } finally
            {
                FCGX_Finish_r(request);
            }

        }
    }
}
