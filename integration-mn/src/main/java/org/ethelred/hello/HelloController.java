package org.ethelred.hello;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-08
 */
@Controller("/hello")
public class HelloController
{
    static Logger log = LoggerFactory.getLogger(HelloController.class);

    public HelloController()
    {
        log.info("HelloController constructed");
    }

    @Get
    @Produces(MediaType.TEXT_PLAIN)
    public String index() {
        log.info("Called /hello");
        return "Hello world!";
    }
}
