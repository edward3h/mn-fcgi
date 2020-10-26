package org.ethelred.html_writer;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-13
 */
public class BaseAttributes<T extends BaseAttributes<T>>
{
    private static final BiFunction<String, String, String> DONT_MERGE = (oldV, newV) -> newV;

    protected static final BaseAttributes<?> EMPTY = new BaseAttributes<>();

    private final Map<String, String> values = new HashMap<>();

    void write(Writer buf) throws IOException
    {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            buf.append(" ").append(k).append("=\"").append(v).append("\"");
        }
    }

    protected T attr(String name, Object value, BiFunction<String, String, String> mergeFunction)
    {
        _validateName(name);
        values.merge(name, _normalizeValue(value), mergeFunction);
        return (T) this;
        
    }

    public T attr(String name, Object value) {
        return attr(name, value, DONT_MERGE);
    }

    private String _normalizeValue(Object value)
    {
        if (value == null) {
            return "";
        }
        return Objects.toString(value).replaceAll("\"", "&quot;");
    }

    private void _validateName(String name)
    {
        // TODO
    }
}
