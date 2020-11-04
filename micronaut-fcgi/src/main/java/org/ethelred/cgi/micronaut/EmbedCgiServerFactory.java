package org.ethelred.cgi.micronaut;

import io.micronaut.context.annotation.Factory;
import org.ethelred.cgi.CgiServer;
import org.ethelred.cgi.graal.CgiServerFactory;

import javax.inject.Singleton;
import java.util.function.Supplier;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-11-03
 */
@Factory
public class EmbedCgiServerFactory
{
    private final CgiServerFactory internal = new CgiServerFactory();

    @Singleton
    public Supplier<CgiServer> getServer() {
        return internal;
    }

}
