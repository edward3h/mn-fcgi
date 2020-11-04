package org.ethelred.cgi.micronaut;

import com.google.common.util.concurrent.Futures;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.http.server.exceptions.HttpServerException;
import io.micronaut.http.server.exceptions.InternalServerException;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.runtime.server.event.ServerShutdownEvent;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import org.ethelred.cgi.CgiHandler;
import org.ethelred.cgi.CgiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-26
 */
@Singleton
public class EmbedCgiServer implements EmbeddedServer, ApplicationEventListener<ServerStartupEvent>
{
    protected static final Logger LOG = LoggerFactory.getLogger(EmbedCgiServer.class);
    private final CgiServer delegate;
    private final ApplicationContext applicationContext;
    private final ApplicationConfiguration applicationConfiguration;
    private final CgiHandler handler;
    private final Future<URI> urlFuture; // pull server info from first request handled - in CGI world, the process gets started after there is a request coming in

    @Inject
    public EmbedCgiServer(Supplier<CgiServer> delegate, ApplicationContext applicationContext, ApplicationConfiguration applicationConfiguration)
    {
        this.delegate = delegate.get();
        this.applicationContext = applicationContext;
        this.applicationConfiguration = applicationConfiguration;
        CompletableFuture<URI> urlCompletableFuture = new CompletableFuture<>();
        this.handler = new EmbedCgiHandler(urlCompletableFuture, applicationContext, applicationConfiguration);
        this.urlFuture = urlCompletableFuture;
        LOG.info("constructed");
    }

    @Override
    public int getPort()
    {
        return getURI().getPort();
    }

    @Override
    public String getHost()
    {
        return getURI().getHost();
    }

    @Override
    public String getScheme()
    {
        return getURI().getScheme();
    }

    @Override
    public URL getURL()
    {
        try
        {
            return getURI().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new InternalServerException(e.getMessage(), e);
        }
    }

    @Override
    public URI getURI()
    {
        LOG.info("getURI {}", urlFuture);
        return Futures.getUnchecked(urlFuture);
    }

    @Override
    public boolean isKeepAlive()
    {
        return false;
    }

    @Override
    public ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    @Override
    public ApplicationConfiguration getApplicationConfiguration()
    {
        return applicationConfiguration;
    }

    @Override
    public boolean isRunning()
    {
        return delegate.isRunning();
    }

    @Nonnull
    @Override
    public EmbeddedServer start()
    {
        try {
            if (!applicationContext.isRunning()) {
                applicationContext.start();
            }
            delegate.init(this::stop);
            //delegate.start(handler);
            applicationContext.publishEventAsync(new ServerStartupEvent(this));
            LOG.info("started");
        } catch (Exception e) {
            LOG.error("startup error", e);
            throw new HttpServerException(
                    "Error starting HTTP server: " + e.getMessage(), e
            );
        }
        return this;
    }

    @Nonnull
    @Override
    public EmbeddedServer stop()
    {
        try {
        delegate.shutdown();
        if (applicationContext.isRunning()) {
            applicationContext.stop();
        }
        applicationContext.publishEvent(new ServerShutdownEvent(this));
        LOG.info("stopped");
    } catch (Exception e) {
            LOG.error("shutdown error", e);
        throw new HttpServerException(
                "Error stopping HTTP server: " + e.getMessage(), e
        );
    }
        return this;
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event)
    {
        LOG.info("Actual delegate start");
        delegate.start(handler);
    }
}
