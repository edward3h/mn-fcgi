package org.ethelred.cgi.micronaut.http;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.cookie.Cookies;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import org.ethelred.cgi.CgiParam;
import org.ethelred.cgi.CgiRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-27
 */
public class CgiCookies implements Cookies
{
    private final Map<CharSequence, Cookie> cookies = new HashMap<>();

    public CgiCookies(CgiRequest request)
    {
        String headerValue = request.getParam(CgiParam.httpHeader(HttpHeaders.COOKIE));
        if (headerValue != null) {
            ServerCookieDecoder.LAX.decode(headerValue)
                    .forEach(nc -> cookies.put(nc.name(), Cookie.of(nc.name(), nc.value())));
        }
    }

    @Override
    public Set<Cookie> getAll()
    {
        return Set.copyOf(cookies.values());
    }

    @Override
    public Set<String> names()
    {
        return cookies.keySet()
                .stream()
                .map(CharSequence::toString)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Cookie> findCookie(CharSequence name)
    {
        return Optional.ofNullable(cookies.get(name));
    }

    @Override
    public Collection<Cookie> values()
    {
        return getAll();
    }

    @Override
    public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext)
    {
        return findCookie(name).flatMap(c -> ConversionService.SHARED.convert(c.getValue(), conversionContext));
    }
}
