package org.ethelred.cgi.micronaut.http;

import com.google.common.io.ByteStreams;
import io.micronaut.core.async.SupplierUtil;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpParameters;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.cookie.Cookies;
import io.micronaut.http.simple.SimpleHttpParameters;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.ethelred.cgi.CgiParam;
import org.ethelred.cgi.CgiRequest;
import org.ethelred.cgi.micronaut.EmbedCgiHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-26
 */
public class CgiHttpRequest implements HttpRequest<Object>
{
    protected static final Logger LOG = LoggerFactory.getLogger(CgiHttpRequest.class);
    private static final HttpParameters EMPTY_HTTP_PARAMETERS = new SimpleHttpParameters(ConversionService.SHARED);
    private final CgiRequest request;
    private final CgiCookies cookies;
    private final CgiHttpHeaders headers;
    private final URI uri;
    private MutableConvertibleValues<Object> attributes;
    private final Supplier<Optional<Object>> body;

    public CgiHttpRequest(CgiRequest request)
    {
        this.request = request;
        this.cookies = new CgiCookies(request);

        String requestContext = request.getParam("REDIRECT_REQUEST_CONTEXT"); // extension param from my rewrite rule
        String requestUri = Objects.requireNonNull(request.getParam(CgiParam.REQUEST_URI), "REQUEST_URI is expected not to be null");
        if (StringUtils.isNotEmpty(requestContext) && requestUri.startsWith(requestContext)) {
            requestUri = requestUri.substring(requestContext.length());
        }
        this.uri = URI.create(requestUri);
        this.headers = new CgiHttpHeaders(request);
        this.body = SupplierUtil.memoizedNonEmpty(() -> Optional.ofNullable(buildBody()));
    }

    private Object buildBody()
    {
        try
        {
            int contentLength = getHeaders().findInt("Content-Length").orElse(0);
            if (contentLength > 0)
            {
                return ByteStreams.toByteArray(request.getBody());
            }
        }
        catch (IOException e)
        {
            LOG.error("Failed to read request body", e);
        }

        return null;
    }

    @Nonnull
    @Override
    public Cookies getCookies()
    {
        return cookies;
    }

    @Nonnull
    @Override
    public HttpParameters getParameters()
    {
        String query = request.getParam(CgiParam.QUERY_STRING);
        if (StringUtils.isEmpty(query)) {
            return EMPTY_HTTP_PARAMETERS;
        }
        QueryStringDecoder decoder = new QueryStringDecoder(URI.create("?" + query));
        var params = decoder.parameters();

        return new SimpleHttpParameters(params.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                ConversionService.SHARED);
    }

    @Nonnull
    @Override
    public HttpMethod getMethod()
    {
        return HttpMethod.parse(request.getParam(CgiParam.REQUEST_METHOD));
    }

    @Nonnull
    @Override
    public URI getUri()
    {
        return uri;
    }

    @Nonnull
    @Override
    public HttpHeaders getHeaders()
    {
        return headers;
    }

    @Nonnull
    @Override
    public MutableConvertibleValues<Object> getAttributes() {
        MutableConvertibleValues<Object> attributes = this.attributes;
        if (attributes == null) {
            synchronized (this) { // double check
                attributes = this.attributes;
                if (attributes == null) {
                    attributes = new MutableConvertibleValuesMap<>(new HashMap<>(4));
                    this.attributes = attributes;
                }
            }
        }
        return attributes;
    }

    @Nonnull
    @Override
    public Optional<Object> getBody()
    {
        return body.get();
    }
}
