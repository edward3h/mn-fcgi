package org.ethelred.hello;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import io.reactivex.Single;

import javax.validation.constraints.NotBlank;

/**
 * @author graemerocher
 * @since 1.0
 */
@Client("/integration-mn")
public interface HelloNameClient {
    @Get("/hello/{name}")
    Single<String> hello(@NotBlank String name);
}