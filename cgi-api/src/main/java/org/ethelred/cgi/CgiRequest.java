package org.ethelred.cgi;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

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
    default String getParam(CgiParam param) {
        return getParam(param.name());
    }

    @Nullable
    InputStream getBody();

    @Nonnull
    OutputStream getOutput();
}
