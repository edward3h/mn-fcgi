package org.ethelred.cgi.graal.libfcgi;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CLibrary;
import org.graalvm.nativeimage.c.type.CCharPointer;

import java.util.List;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-09-29
 */
@CContext(LibFCGI.LibFCGIDirectives.class)
@CLibrary("fcgi")
public class LibFCGI
{
    static class LibFCGIDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles()
        {
            return List.of("<fcgiapp.h>");
        }
    }

    @CFunction
    public static native int FCGX_IsCGI();

    @CFunction
    public static native int FCGX_Init();

    @CFunction
    public static native void FCGX_ShutdownPending();

    @CFunction
    public static native int FCGX_OpenSocket(CCharPointer path, int backlog);

    @CFunction
    public static native int FCGX_InitRequest(FCGX_Request request, int sockFD, int flags);

    @CFunction
    public static native int FCGX_Accept_r(FCGX_Request request);

    @CFunction
    public static native void FCGX_Finish_r(FCGX_Request request);

    @CFunction
    public static native void FCGX_Free(FCGX_Request request, int close);

    @CFunction
    public static native CCharPointer FCGX_GetParam(CCharPointer name, FCGX_ParamArray envp);

    @CFunction
    public static native int FCGX_GetChar(FCGX_Stream stream);

    @CFunction
    public static native int FCGX_GetStr(CCharPointer str, int n, FCGX_Stream stream);

    @CFunction
    public static native CCharPointer FCGX_GetLine(CCharPointer str, int n, FCGX_Stream stream);

    @CFunction
    public static native int FCGX_HasSeenEOF(FCGX_Stream stream);

    @CFunction
    public static native int FCGX_PutChar(int c, FCGX_Stream stream);

    @CFunction
    public static native int FCGX_PutStr(CCharPointer str, int n, FCGX_Stream stream);
    @CFunction
    public static native int FCGX_PutS(CCharPointer str, FCGX_Stream stream);

    @CFunction
    public static native int FCGX_GetError(FCGX_Stream stream);
    @CFunction
    public static native void FCGX_ClearError(FCGX_Stream stream);

}
