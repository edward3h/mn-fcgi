package org.ethelred.cgi;

/**
 * See https://tools.ietf.org/html/rfc3875, http://www.cgi101.com/book/ch3/text.html
 *
 * @author eharman
 * @since 2020-10-12
 */
public enum CgiParam implements ParamName
{
    // have not yet attempted to add every supported variable name
    REQUEST_METHOD,
    QUERY_STRING,
    CONTENT_LENGTH, // set if and only if there is a request body
    REQUEST_URI, CONTENT_TYPE, SERVER_PROTOCOL, SERVER_NAME, SERVER_PORT;


    @Override
    public String getName()
    {
        return name();
    }

    public static ParamName httpHeader(CharSequence headerName) {
        return () -> "HTTP_" + headerName.toString().toUpperCase();
    };
}
