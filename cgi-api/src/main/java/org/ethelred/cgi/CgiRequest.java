package org.ethelred.cgi;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-11
 */
public interface CgiRequest
{
    @Nonnull
    Map<String, String> getEnv();

    @CheckForNull
    default String getParam(String key) {
        return getEnv().get(key);
    }

    @CheckForNull
    default String getParam(ParamName param) {
        return getParam(param.getName());
    }

    @Nonnull
    default String getRequiredParam(ParamName param) {
        return Objects.requireNonNull(getParam(param), "Missing required parameter " + param.getName());
    }

    @Nonnull
    default Optional<String> getOptionalParam(ParamName param) {
        return Optional.ofNullable(getParam(param));
    }

    @Nullable
    InputStream getBody();

    @Nonnull
    OutputStream getOutput();
}
