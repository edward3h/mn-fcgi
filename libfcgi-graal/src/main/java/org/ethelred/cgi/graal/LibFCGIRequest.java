package org.ethelred.cgi.graal;

import org.ethelred.cgi.CgiRequest;
import org.ethelred.cgi.graal.libfcgi.FCGX_ParamArray;
import org.ethelred.cgi.graal.libfcgi.FCGX_Request;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-12
 */
public class LibFCGIRequest implements CgiRequest
{
    private final FCGX_Request request;
    private final Map<String, String> env;
    private final FCGXInputStream in;
    private final FCGXOutputStream out;

    protected LibFCGIRequest(FCGX_Request request)
    {
        this.request = request;
        env = _getEnvp();
        in = new FCGXInputStream(request.getIn());
        out = new FCGXOutputStream(request.getOut());
    }

    @Nonnull
    @Override
    public Map<String, String> getEnv()
    {
        return env;
    }

    @Nonnull
    @Override
    public InputStream getBody()
    {
        return in;
    }

    @Nonnull
    @Override
    public OutputStream getOutput()
    {
        return out;
    }

    private Map<String, String> _getEnvp() {
        Map<String, String> result = new HashMap<>();
        FCGX_ParamArray params = request.getEnvp();
        for (int i = 0; ; i++) {
            CCharPointer charPointer = params.read(i);
            if (charPointer.isNull()) {
                break;
            }
            String entry = CTypeConversion.toJavaString(charPointer);
            String[] k_v = entry.split("=", 2);
            result.put(k_v[0], k_v[1]);
        }
        return result;
    }
}
