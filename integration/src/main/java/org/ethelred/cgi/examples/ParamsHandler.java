package org.ethelred.cgi.examples;

import org.ethelred.cgi.CgiHandler;
import org.ethelred.cgi.CgiParam;
import org.ethelred.cgi.CgiRequest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-26
 */
public class ParamsHandler implements CgiHandler
{
    @Override
    public void handleRequest(CgiRequest request)
    {
        int counter = 1;
        String counterValue = _getCookie(request, "counter");
        if (counterValue != null) {
            try
            {
                counter = Integer.parseInt(counterValue);
                counter++;
            } catch (NumberFormatException ignore) {
                // ignore
            }
        }
        PrintStream out = new PrintStream(request.getOutput());
        out.println("Content-type:text/plain");
        out.printf("Set-Cookie: counter=%d; Max-Age=%d%n", counter, 60 * 12);
        out.printf("Set-Cookie: flavour=Chocolate_Chip; Max-Age=%d%n", 60 * 60);
        out.println();
        out.println("Params");

        request.getEnv().entrySet()
                .stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> {
            out.printf("%s=%s%n", e.getKey(), e.getValue());
        });
        String cl = request.getParam(CgiParam.CONTENT_LENGTH);
        try {
            if (cl != null && Integer.parseInt(cl) > 0) {
                out.println("Body");
                InputStream in = request.getBody();
                if (in != null)
                {
                    BufferedInputStream inputStream = new BufferedInputStream(in);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > -1)
                    {
                        out.write(buf, 0, len);
                    }
                    out.println();
                }
            }
        } catch (NumberFormatException | IOException ignore) {
            // ignore
        }

    }

    private String _getCookie(CgiRequest request, String name)
    {
        String header = request.getParam(CgiParam.httpHeader("cookie"));
        if (header != null) {
            Pattern p = Pattern.compile("\\b" + name + "=([^;]*)");
            Matcher m = p.matcher(header);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }
}
