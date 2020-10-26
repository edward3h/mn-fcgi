package org.ethelred.cgi;

/**
 * See https://tools.ietf.org/html/rfc3875
 *
 * @author eharman
 * @since 2020-10-12
 */
public enum CgiParam
{
    // have not yet attempted to add every supported variable name
    REQUEST_METHOD,
    PATH_INFO,
    QUERY_STRING;
}
