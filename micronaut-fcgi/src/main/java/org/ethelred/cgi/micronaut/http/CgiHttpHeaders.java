package org.ethelred.cgi.micronaut.http;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.http.HttpHeaders;
import org.ethelred.cgi.CgiParam;
import org.ethelred.cgi.CgiRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ref: https://tools.ietf.org/html/rfc3875#section-4.1.18
 *
 * @author eharman
 * @since 2020-10-29
 */
public class CgiHttpHeaders implements HttpHeaders
{
    private static final Set<String> SEPARATE_PARAMS =
            Set.of(CgiParam.CONTENT_TYPE, CgiParam.CONTENT_LENGTH) // TODO other params
            .stream().map(CgiParam::getName).collect(Collectors.toSet());
    private static final String _PREFIX = "HTTP_";
    private final Map<String, String> env;

    private String _normalize(CharSequence name) {
        Objects.requireNonNull(name, "Header name may not be null");
        String normal = name.toString().toUpperCase().replaceAll("-", "_");
        if (SEPARATE_PARAMS.contains(normal)) {
            return normal;
        }
        return _PREFIX + normal;
    }

    public CgiHttpHeaders(CgiRequest request)
    {
        this.env = request.getEnv();
    }

    @Override
    public List<String> getAll(CharSequence name)
    {
        String v = env.get(_normalize(name));
        if (v != null) {
            return _asList(v); // CGI spec says that multiple of same header are combined by the server
        }
        return List.of();
    }

    @Nonnull
    private List<String> _asList(String v)
    {
        return List.of(v.split(", ?"));
    }

    @Nullable
    @Override
    public String get(CharSequence name)
    {
        return env.get(_normalize(name));
    }

    @Override
    public Set<String> names()
    {
        return env.keySet().stream()
                .filter(k -> SEPARATE_PARAMS.contains(k) || k.startsWith(_PREFIX))
                .map(k -> {
                    if (SEPARATE_PARAMS.contains(k)) {
                        return k;
                    } else {
                        return k.substring(_PREFIX.length());
                    }
                }).collect(Collectors.toSet());
    }

    @Override
    public Collection<List<String>> values()
    {
        return env.values().stream()
                .map(this::_asList)
                .collect(Collectors.toList());
    }

    @Override
    public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext)
    {
        return Optional.ofNullable(get(name)).flatMap(x -> ConversionService.SHARED.convert(x, conversionContext));
    }
}
