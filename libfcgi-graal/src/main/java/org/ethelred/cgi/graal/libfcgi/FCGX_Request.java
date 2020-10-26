package org.ethelred.cgi.graal.libfcgi;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-09-30
 */
@CContext(LibFCGI.LibFCGIDirectives.class)
@CStruct("FCGX_Request")
public interface FCGX_Request extends PointerBase
{
    @CField("envp")
    FCGX_ParamArray getEnvp();

    @CField("in")
    FCGX_Stream getIn();

    @CField("out")
    FCGX_Stream getOut();

    @CField("err")
    FCGX_Stream getErr();
}
