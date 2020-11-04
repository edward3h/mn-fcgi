package org.ethelred.cgi.micronaut.http;

import com.google.common.base.Joiner;
import io.micronaut.core.async.SupplierUtil;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpHeaders;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.simple.SimpleHttpHeaders;
import io.micronaut.http.simple.cookies.SimpleCookies;
import org.ethelred.cgi.CgiRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-26
 */
public class CgiHttpResponse implements MutableHttpResponse<Object>
{
    private final static Logger LOG = LoggerFactory.getLogger(CgiHttpResponse.class);

    private final MutableHttpHeaders headers = new SimpleHttpHeaders(ConversionService.SHARED);
    private final SimpleCookies cookies = new SimpleCookies(ConversionService.SHARED);
    private final MutableConvertibleValues<Object> attributes = new MutableConvertibleValuesMap<>();
    private final CgiRequest request;
    private final AtomicBoolean wroteHeaders = new AtomicBoolean(false);
    private final Supplier<PrintStream> _out;
    private final Supplier<BufferedWriter> _writer;
    private HttpStatus status = HttpStatus.OK;
    @Nullable
    private Object body;

    public CgiHttpResponse(CgiRequest request)
    {
        this.request = request;
        // TODO charset?
        _out = SupplierUtil.memoizedNonEmpty(() -> new PrintStream(request.getOutput()));
        _writer = SupplierUtil.memoizedNonEmpty(() -> new BufferedWriter(new OutputStreamWriter(_out.get())));
    }

    public BufferedWriter getWriter()
    {
        LOG.debug("getWriter");
        _checkAndWriteHeaders();
        return _writer.get();
    }

    public OutputStream getOutputStream()
    {
        LOG.debug("getOutputStream");
        _checkAndWriteHeaders();
        return _out.get();
    }

    private void _checkAndWriteHeaders()
    {
        if (wroteHeaders.compareAndSet(false, true)) {
            //headers.contentType().ifPresent(mediaType -> _writeHeader(HttpHeaders.CONTENT_TYPE, mediaType));
            _writeHeader("Status", status.getCode());
            headers.forEach((k, values) -> _writeHeader(k, Joiner.on(", ").join(values)));
            // TODO set-cookie?
            _out.get().println();
        }
    }

    private void _writeHeader(String name, Object value)
    {
        _out.get().println(name + ": " + value.toString());
    }

    @Override
    public MutableHttpResponse<Object> cookie(Cookie cookie)
    {
        cookies.put(cookie.getName(), cookie);
        return this;
    }

    @Nonnull
    @Override
    public MutableHttpHeaders getHeaders()
    {
        return headers;
    }

    @Nonnull
    @Override
    public MutableConvertibleValues<Object> getAttributes()
    {
        return attributes;
    }

    @Nonnull
    @Override
    public Optional<Object> getBody()
    {
        return Optional.ofNullable(body);
    }

    @Override
    public <T> MutableHttpResponse<T> body(@Nullable T body)
    {
        this.body = body;
        return (MutableHttpResponse<T>) this;
    }

    @Override
    public MutableHttpResponse<Object> status(HttpStatus status, CharSequence message)
    {
        this.status = status;
        // TODO ignoring message for now
        return this;
    }

    @Override
    public HttpStatus getStatus()
    {
        return status;
    }
}
