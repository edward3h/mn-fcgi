package org.ethelred.cgi.micronaut;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.io.Writable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpAttributes;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.codec.CodecException;
import io.micronaut.http.codec.MediaTypeCodec;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.filter.HttpFilter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.OncePerRequestHttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.server.binding.RequestArgumentSatisfier;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.InternalServerException;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.web.router.RouteMatch;
import io.micronaut.web.router.Router;
import io.micronaut.web.router.UriRoute;
import io.micronaut.web.router.UriRouteMatch;
import io.micronaut.web.router.exceptions.DuplicateRouteException;
import io.micronaut.web.router.exceptions.UnsatisfiedRouteException;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.ethelred.cgi.CgiHandler;
import org.ethelred.cgi.CgiParam;
import org.ethelred.cgi.CgiRequest;
import org.ethelred.cgi.micronaut.http.CgiHttpRequest;
import org.ethelred.cgi.micronaut.http.CgiHttpResponse;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @author eharman
 * @since 2020-10-26
 */
public class EmbedCgiHandler implements CgiHandler
{
    protected static final Logger LOG = LoggerFactory.getLogger(EmbedCgiHandler.class);

    private final ApplicationContext applicationContext;
    private final ApplicationConfiguration applicationConfiguration;
    private final Router router;
    private final RequestArgumentSatisfier requestArgumentSatisfier;
    private final MediaTypeCodecRegistry mediaTypeCodecRegistry;
    private final CompletableFuture<URI> urlCallback;

    public EmbedCgiHandler(CompletableFuture<URI> urlCallback, ApplicationContext applicationContext, ApplicationConfiguration applicationConfiguration)
    {
        this.applicationContext = Objects.requireNonNull(applicationContext, "The application context cannot be null");
        this.applicationConfiguration = applicationConfiguration;
        this.router = applicationContext.getBean(Router.class);
        this.requestArgumentSatisfier = applicationContext.getBean(RequestArgumentSatisfier.class);
        this.mediaTypeCodecRegistry = applicationContext.getBean(MediaTypeCodecRegistry.class);
        this.urlCallback = urlCallback;
    }

    @Override
    public void handleRequest(CgiRequest request)
    {
        final long time = System.currentTimeMillis();
        _callback(request);
        final MutableHttpResponse<Object> res = new CgiHttpResponse(request);
        final HttpRequest<Object> req = new CgiHttpRequest(request);
        LOG.info("new request {} {}", req.getMethodName(), req.getPath());
        try {
            final List<UriRouteMatch<Object, Object>> matchingRoutes = router.findAllClosest(req);
            if (CollectionUtils.isNotEmpty(matchingRoutes)) {
                RouteMatch<Object> route;
                if (matchingRoutes.size() > 1) {
                    throw new DuplicateRouteException(req.getPath(), matchingRoutes);
                } else {
                    UriRouteMatch<Object, Object> establishedRoute = matchingRoutes.get(0);
                    req.setAttribute(HttpAttributes.ROUTE, establishedRoute.getRoute());
                    req.setAttribute(HttpAttributes.ROUTE_MATCH, establishedRoute);
                    req.setAttribute(HttpAttributes.URI_TEMPLATE, establishedRoute.getRoute().getUriMatchTemplate().toString());
                    route = establishedRoute;
                }


                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} - {} - routed to controller {}", req.getMethodName(), req.getPath(), route.getDeclaringType().getSimpleName());
                    traceHeaders(req.getHeaders());
                }

                invokeRouteMatch(req, res, route, false);

            } else {

                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} - {} - No matching routes found", req.getMethodName(), req.getPath());
                    traceHeaders(req.getHeaders());
                }

                Set<String> existingRouteMethods = router
                        .findAny(req.getUri().toString(), req)
                        .map(UriRouteMatch::getRoute)
                        .map(UriRoute::getHttpMethodName)
                        .collect(Collectors.toSet());

                if (CollectionUtils.isNotEmpty(existingRouteMethods)) {
                    if (existingRouteMethods.contains(req.getMethodName())) {
                        MediaType contentType = req.getContentType().orElse(null);
                        if (contentType != null) {
                            // must be invalid mime type
                            boolean invalidMediaType = router.findAny(req.getUri().toString(), req)
                                    .anyMatch(rm -> rm.accept(contentType));
                            if (!invalidMediaType) {
                                handleStatusRoute(res, req, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                            } else {
                                handlePageNotFound(res, req);
                            }

                        } else {
                            handlePageNotFound(res, req);
                        }
                    } else {
                        final RouteMatch<Object> notAllowedRoute =
                                router.route(HttpStatus.METHOD_NOT_ALLOWED).orElse(null);

                        if (notAllowedRoute != null) {
                            invokeRouteMatch(req, res, notAllowedRoute, true);
                        } else {
                            emitError(res, req, emitter -> {
                                res.getHeaders().allowGeneric(existingRouteMethods);
                                res.status(HttpStatus.METHOD_NOT_ALLOWED)
                                        .body(new JsonError(
                                                "Method [" + req.getMethod() + "] not allowed for URI [" + req
                                                        .getPath() + "]. Allowed methods: " + existingRouteMethods
                                        ));
                                emitter.onNext(res);
                                emitter.onComplete();
                            });
                        }
                    }
                } else {
                    handlePageNotFound(res, req);
                }
            }
        } finally {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Executed HTTP Request [{} {}] in: {}ms",
                        req.getMethod(),
                        req.getPath(),
                        (System.currentTimeMillis() - time)
                );
            }
        }
    }

    private void _callback(CgiRequest request)
    {
        LOG.info("_callback {} {}", urlCallback, urlCallback.isDone());
        if (!urlCallback.isDone()) {
            String contextPath = "/";
            // CGI standard doesn't know about request context - this is my implementation
            String requestContext = request.getParam("REDIRECT_REQUEST_CONTEXT");
            if (StringUtils.isNotEmpty(requestContext)) {
                contextPath = requestContext;
            }
            try
            {
                // new URI(scheme, null, host, connector.getLocalPort(), path, null, null);
                URI url = new URI(
                        "http",// TODOrequest.getRequiredParam(CgiParam.SERVER_PROTOCOL),
                        null,
                        request.getRequiredParam(CgiParam.SERVER_NAME),
                        Integer.parseInt(request.getRequiredParam(CgiParam.SERVER_PORT)),
                        contextPath,
                        null,
                        null
                );
                urlCallback.complete(url);
                LOG.info("_callback complete {}  {}", urlCallback, url);
            }
            catch (NullPointerException | URISyntaxException e)
            {
                urlCallback.completeExceptionally(e);
                LOG.error("_callback ", e);
                throw new InternalServerException(e.getMessage(), e);
            }
        }
    }

    private void traceHeaders(HttpHeaders httpHeaders) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("-----");
            httpHeaders.forEach((name, values) -> LOG.trace("{} : {}", name, values));
            LOG.trace("-----");
        }
    }

    private void invokeRouteMatch(
            HttpRequest<Object> req,
            MutableHttpResponse<Object> res,
            final RouteMatch<?> route,
            boolean isErrorRoute) {

        try {

            Publisher<? extends MutableHttpResponse<?>> responsePublisher = buildResponsePublisher(req, res, route, isErrorRoute);
            final AnnotationMetadata annotationMetadata = route.getAnnotationMetadata();
            subscribeToResponsePublisher(req, res, route, isErrorRoute, responsePublisher, annotationMetadata);
        } catch (Throwable e) {
            handleException(req, res, route, isErrorRoute, e);
        }
    }

    private Publisher<? extends MutableHttpResponse<?>> buildResponsePublisher(
            HttpRequest<Object> req,
            MutableHttpResponse<Object> res,
            RouteMatch<?> route,
            boolean isErrorRoute) {
        Publisher<? extends MutableHttpResponse<?>> responsePublisher
                = Flowable.<MutableHttpResponse<?>>defer(() -> {
            RouteMatch<?> computedRoute = route;
            if (!computedRoute.isExecutable()) {
                computedRoute = requestArgumentSatisfier.fulfillArgumentRequirements(
                        computedRoute,
                        req,
                        false
                );
            }
            if (!computedRoute.isExecutable() && HttpMethod.permitsRequestBody(req.getMethod()) && computedRoute.getBodyArgument().isEmpty()) {
                final ConvertibleValues<?> convertibleValues = req.getBody(ConvertibleValues.class).orElse(null);
                if (convertibleValues != null) {

                    final Collection<Argument> requiredArguments = route.getRequiredArguments();
                    Map<String, Object> newValues = new HashMap<>(requiredArguments.size());
                    for (Argument<?> requiredArgument : requiredArguments) {
                        final String name = requiredArgument.getName();
                        convertibleValues.get(name, requiredArgument).ifPresent(v -> newValues.put(name, v));
                    }
                    if (CollectionUtils.isNotEmpty(newValues)) {
                        computedRoute = computedRoute.fulfill(
                                newValues
                        );
                    }
                }
            }

            RouteMatch<?> finalComputedRoute = computedRoute;
            Object result = ServerRequestContext.with(req, (Callable<Object>) finalComputedRoute::execute);
            if (result instanceof Optional) {
                result = ((Optional<?>) result).orElse(null);
            }
            MutableHttpResponse<Object> httpResponse = res;
            if (result instanceof MutableHttpResponse) {
                httpResponse = (MutableHttpResponse<Object>) result;
                result = httpResponse.body();
            }
            final ReturnType<?> returnType = computedRoute.getReturnType();
            final Argument<?> genericReturnType = returnType.asArgument();
            final Class<?> javaReturnType = returnType.getType();
            if (result == null) {
                boolean isVoid = javaReturnType == void.class ||
                        Completable.class.isAssignableFrom(javaReturnType) ||
                        (genericReturnType.getFirstTypeVariable()
                                .map(arg -> arg.getType() == Void.class).orElse(false));
                if (isVoid) {
                    return Publishers.just(httpResponse);
                } else {
                    if (httpResponse.status() == HttpStatus.OK) {
                        httpResponse.status(HttpStatus.NOT_FOUND);
                    }
                    return Publishers.just(httpResponse);
                }
            }

            Argument<?> firstArg = genericReturnType.getFirstTypeVariable().orElse(null);
            if (result instanceof Future) {
                if (result instanceof CompletionStage) {
                    CompletionStage<?> cs = (CompletionStage<?>) result;
                    result = Maybe.create(emitter -> cs.whenComplete((o, throwable) -> {
                        if (throwable != null) {
                            emitter.onError(throwable);
                        } else {
                            if (o != null) {
                                emitter.onSuccess(o);
                            } else {
                                emitter.onComplete();
                            }
                        }
                    }));
                } else {
                    result = Single.fromFuture((Future<?>) result);
                }
            }

            if (firstArg != null && HttpResponse.class.isAssignableFrom(firstArg.getType()) && Publishers.isConvertibleToPublisher(result)) {
                //noinspection unchecked
                return Publishers.convertPublisher(result, Flowable.class);
            } else {
                return Publishers.just(
                        httpResponse.body(result)
                );
            }
        });
        return filterPublisher(new AtomicReference<>(req), responsePublisher, isErrorRoute);
    }

    private Publisher<? extends MutableHttpResponse<?>> filterPublisher(
            AtomicReference<io.micronaut.http.HttpRequest<?>> requestReference,
            Publisher<? extends MutableHttpResponse<?>> routePublisher,
            boolean skipOncePerRequest) {
        Publisher<? extends io.micronaut.http.MutableHttpResponse<?>> finalPublisher;
        List<HttpFilter> filters = new ArrayList<>(router.findFilters(requestReference.get()));
        if (filters.isEmpty()) {
            return routePublisher;
        }
        if (skipOncePerRequest) {
            filters.removeIf(filter -> filter instanceof OncePerRequestHttpServerFilter);
        }
        if (!filters.isEmpty()) {
            // make the action executor the last filter in the chain
            //noinspection unchecked
            filters.add((HttpServerFilter) (req, chain) -> (Publisher<MutableHttpResponse<?>>) routePublisher);

            AtomicInteger integer = new AtomicInteger();
            int len = filters.size();
            ServerFilterChain filterChain = new ServerFilterChain() {
                @SuppressWarnings("unchecked")
                @Override
                public Publisher<MutableHttpResponse<?>> proceed(io.micronaut.http.HttpRequest<?> request) {
                    int pos = integer.incrementAndGet();
                    if (pos > len) {
                        throw new IllegalStateException("The FilterChain.proceed(..) method should be invoked exactly once per filter execution. The method has instead been invoked multiple times by an erroneous filter definition.");
                    }
                    HttpFilter httpFilter = filters.get(pos);
                    return (Publisher<MutableHttpResponse<?>>) httpFilter.doFilter(requestReference.getAndSet(request), this);
                }
            };
            HttpFilter httpFilter = filters.get(0);
            final HttpRequest<?> req = requestReference.get();
            Publisher<? extends io.micronaut.http.HttpResponse<?>> resultingPublisher = ServerRequestContext
                    .with(req, (Supplier<Publisher<? extends HttpResponse<?>>>) () -> httpFilter.doFilter(req, filterChain));
            //noinspection unchecked
            finalPublisher = (Publisher<? extends MutableHttpResponse<?>>) resultingPublisher;
        } else {
            finalPublisher = routePublisher;
        }
        return finalPublisher;
    }

    private void subscribeToResponsePublisher(HttpRequest<Object> req,
                                              MutableHttpResponse<Object> res,
                                              RouteMatch<?> route,
                                              boolean isErrorRoute,
                                              Publisher<? extends MutableHttpResponse<?>> responsePublisher,
                                              AnnotationMetadata annotationMetadata) {
        final Flowable<? extends MutableHttpResponse<?>> responseFlowable = Flowable.fromPublisher(responsePublisher)
                .flatMap(response -> {
                    final HttpStatus status = response.status();
                    Object body = response.body();

                    if (body != null) {
                        if (Publishers.isConvertibleToPublisher(body)) {
                            boolean isSingle = Publishers.isSingle(body.getClass());
                            if (isSingle) {
                                Flowable<?> flowable = Publishers.convertPublisher(body, Flowable.class);
                                return flowable.map((Function<Object, MutableHttpResponse<?>>) o -> {
                                    if (o instanceof CgiHttpResponse) {
                                        encodeResponse(annotationMetadata, (CgiHttpResponse) o);
                                        return res;
                                    } else {
                                        res.body(o);
                                        encodeResponse(annotationMetadata, (CgiHttpResponse) response);
                                        return res;
//                                        LOG.error("What is o {} {} what is response {} what is res {}", o.getClass(), o, response, res);
//                                        throw new IllegalStateException("I don't know what should happen here");
                                    }
                                }).switchIfEmpty(Flowable.defer(() -> {
                                    final RouteMatch<Object> errorRoute = lookupStatusRoute(route, HttpStatus.NOT_FOUND);
                                    if (errorRoute != null) {
                                        Flowable<MutableHttpResponse<?>> notFoundFlowable = Flowable.fromPublisher(buildResponsePublisher(
                                                req,
                                                (MutableHttpResponse<Object>) response,
                                                errorRoute,
                                                true
                                        ));
                                        return notFoundFlowable.onErrorReturn(throwable -> {

                                            if (LOG.isErrorEnabled()) {
                                                LOG.error("Error occuring invoking 404 handler: " + throwable.getMessage());
                                            }
                                            MutableHttpResponse<Object> defaultNotFound = res.status(404).body(newJsonError(req, "Page Not Found"));
                                            encodeResponse(annotationMetadata, (CgiHttpResponse) defaultNotFound);
                                            return defaultNotFound;
                                        });
                                    } else {
                                        return Publishers.just(res.status(404).body(newJsonError(req, "Page Not Found")));
                                    }
                                }));
                            } else {
                                // stream case
                                Flowable<?> flowable = Publishers.convertPublisher(body, Flowable.class);
                                    // fallback to blocking
                                    return flowable.toList().map(list -> {
                                        encodeResponse(annotationMetadata, res.body(list));
                                        return res;
                                    }).toFlowable();
                            }
                        } else {

                            if (!isErrorRoute && status.getCode() >= 400) {
                                final RouteMatch<Object> errorRoute = lookupStatusRoute(route, status);
                                if (errorRoute != null) {
                                    return buildErrorRouteHandler(req, (MutableHttpResponse<Object>) response, errorRoute);
                                }
                            }
                        }
                    }

//                    if (body != null) {
//                        Class<?> bodyType = body.getClass();
//                        ServletResponseEncoder<Object> responseEncoder = (ServletResponseEncoder<Object>) responseEncoders.get(bodyType);
//                        if (responseEncoder != null) {
//                            return responseEncoder.encode(exchange, annotationMetadata, body);
//                        }
//                    }

                    if (!isErrorRoute && status.getCode() >= 400) {
                        final RouteMatch<Object> errorRoute = lookupStatusRoute(route, status);
                        if (errorRoute != null) {
                            return buildErrorRouteHandler( req, (MutableHttpResponse<Object>) response, errorRoute);
                        }
                    }

                    return Flowable.fromCallable(() -> {
                        encodeResponse( annotationMetadata, (MutableHttpResponse<Object>) response);
                        return response;
                    });
                }).onErrorResumeNext(throwable -> {
                    handleException(req, res, route, isErrorRoute, throwable);
                    return Flowable.error(throwable);
                });
            responseFlowable
                    .blockingSubscribe(response -> {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Request [{} - {}] completed successfully", req.getMethodName(), req.getUri());
                        }
                    }, throwable -> {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Request [" + req.getMethodName() + " - " + req.getUri() + "] completed with error: " + throwable.getMessage(), throwable);
                        }
                    });
    }


    private Publisher<? extends MutableHttpResponse<?>> buildErrorRouteHandler(
            HttpRequest<Object> request,
            MutableHttpResponse<Object> response,
            RouteMatch<Object> errorRoute) {
        return Publishers.map(buildResponsePublisher(
                request,
                response,
                errorRoute,
                true
        ), servletResponse -> {
            encodeResponse( errorRoute.getAnnotationMetadata(), (MutableHttpResponse<Object>) servletResponse);
            return servletResponse;
        });
    }

    private void encodeResponse(AnnotationMetadata annotationMetadata, MutableHttpResponse<Object> response) {
        final Object body = response.getBody().orElse(null);
        setHeadersFromMetadata(response, annotationMetadata, body);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending response {}", response.status());
            traceHeaders(response.getHeaders());
        }
        if (body instanceof HttpStatus) {
            HttpResponse.status((HttpStatus) body);
        } else if (body instanceof CharSequence) {
            if (response.getContentType().isEmpty()) {
                ((MutableHttpResponse<?>) response).contentType(MediaType.TEXT_PLAIN_TYPE);
            }
            try (BufferedWriter writer = ((CgiHttpResponse) response).getWriter()) {
                writer.write(body.toString());
                writer.flush();
            } catch (IOException e) {
                throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else if (body instanceof byte[]) {
            try (OutputStream outputStream = ((CgiHttpResponse) response).getOutputStream()) {
                outputStream.write((byte[]) body);
                outputStream.flush();
            } catch (IOException e) {
                throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else if (body instanceof Writable) {
            Writable writable = (Writable) body;
            try (OutputStream outputStream = ((CgiHttpResponse) response).getOutputStream()) {
                writable.writeTo(outputStream, response.getCharacterEncoding());
                outputStream.flush();
            } catch (IOException e) {
                throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else if (body != null) {
            Class<?> bodyType = body.getClass();
            final MediaType ct = response.getContentType().orElseGet(() -> {
                final Produces ann = bodyType.getAnnotation(Produces.class);
                if (ann != null) {
                    final String[] v = ann.value();
                    if (ArrayUtils.isNotEmpty(v)) {
                        final MediaType mediaType = new MediaType(v[0]);
                        ((MutableHttpResponse<?>) response).contentType(mediaType);
                        return mediaType;
                    }
                }
                ((MutableHttpResponse<?>) response).contentType(MediaType.APPLICATION_JSON_TYPE);
                return MediaType.APPLICATION_JSON_TYPE;
            });
            final MediaTypeCodec codec = mediaTypeCodecRegistry.findCodec(ct, bodyType).orElse(null);
            if (codec != null) {
                try (OutputStream outputStream = ((CgiHttpResponse) response).getOutputStream()) {
                    codec.encode(body, outputStream);
                    outputStream.flush();
                } catch (Throwable e) {
                    throw new CodecException("Failed to encode object [" + body + "] to content type [" + ct + "]: " + e.getMessage(), e);
                }
            } else {
                throw new CodecException("No codec present capable of encoding object [" + body + "] to content type [" + ct + "]");
            }
        }
    }

    private void setHeadersFromMetadata(MutableHttpResponse<Object> res, AnnotationMetadata annotationMetadata, Object result) {
        if (res.getContentType().isEmpty()) {
            final String contentType = annotationMetadata.stringValue(Produces.class)
                    .orElse(getDefaultMediaType(result));
            if (contentType != null) {
                res.contentType(contentType);
            } else if (result instanceof CharSequence) {
                res.contentType(MediaType.TEXT_PLAIN);
            }
        }

        annotationMetadata.enumValue(Status.class, HttpStatus.class)
                .ifPresent(res::status);
        final List<AnnotationValue<Header>> headers = annotationMetadata.getAnnotationValuesByType(Header.class);
        for (AnnotationValue<Header> header : headers) {
            final String value = header.stringValue().orElse(null);
            final String name = header.stringValue("name").orElse(null);
            if (name != null && value != null) {
                res.header(name, value);
            }
        }
    }

    private String getDefaultMediaType(Object result) {
        if (result instanceof CharSequence) {
            return MediaType.TEXT_PLAIN;
        } else if (result != null) {
            return MediaType.APPLICATION_JSON;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ExceptionHandler<Throwable, ?> lookupExceptionHandler(Throwable e) {
        final Class<? extends Throwable> type = e.getClass();
        return applicationContext.findBean(ExceptionHandler.class, Qualifiers.byTypeArgumentsClosest(type, Object.class))
                .orElse(null);
    }

    private RouteMatch<Object> lookupErrorRoute(RouteMatch<?> route, Throwable e) {
        if (route == null) {
            return router.route(e).orElse(null);
        } else {
            return router.route(route.getDeclaringType(), e)
                    .orElseGet(() -> router.route(e).orElse(null));
        }
    }

    private RouteMatch<Object> lookupStatusRoute(RouteMatch<?> route, HttpStatus status) {
        if (route == null) {
            return router.route(status).orElse(null);
        } else {
            return router.route(route.getDeclaringType(), status)
                    .orElseGet(() ->
                            router.route(status).orElse(null)
                    );
        }
    }


    private void handleException(
            HttpRequest<Object> req,
            MutableHttpResponse<Object> res,
            RouteMatch<?> route,
            boolean isErrorRoute,
            Throwable e) {
        req.setAttribute(HttpAttributes.ERROR, e);
        if (isErrorRoute) {
            // handle error default
            if (LOG.isErrorEnabled()) {
                LOG.error("Error occurred executing Error route [" + route + "]: " + e.getMessage(), e);
            }
            res.status(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } else {
            if (e instanceof UnsatisfiedRouteException || e instanceof ConversionErrorException) {
                final RouteMatch<Object> badRequestRoute = lookupStatusRoute(route, HttpStatus.BAD_REQUEST);
                if (badRequestRoute != null) {
                    invokeRouteMatch(req, res, badRequestRoute, true);
                } else {
                    invokeExceptionHandlerIfPossible(req, res, e, HttpStatus.BAD_REQUEST);
                }
            } else if (e instanceof HttpStatusException) {
                HttpStatusException statusException = (HttpStatusException) e;
                final HttpStatus status = statusException.getStatus();
                final int code = status.getCode();
                final boolean isErrorStatus = code >= 400;
                final RouteMatch<Object> statusRoute = isErrorStatus ? lookupStatusRoute(route, status) : null;
                if (statusRoute != null) {
                    invokeRouteMatch(req, res, statusRoute, true);
                } else {
                    emitError( res, req, (emitter -> {
                        res.status(code, statusException.getMessage());
                        final Object body = statusException.getBody().orElse(null);
                        if (body != null) {
                            res.body(body);
                        } else if (isErrorStatus) {
                            res.body(newJsonError(req, statusException.getMessage()));
                        }
                        emitter.onNext(res);
                        emitter.onComplete();
                    }));
                }

            } else {

                RouteMatch<Object> errorRoute = lookupErrorRoute(route, e);
                if (errorRoute == null) {
                    if (e instanceof CodecException) {
                        Throwable cause = e.getCause();
                        if (cause != null) {
                            errorRoute = lookupErrorRoute(route, cause);
                        }
                        if (errorRoute == null) {
                            final RouteMatch<Object> badRequestRoute = lookupStatusRoute(route, HttpStatus.BAD_REQUEST);
                            if (badRequestRoute != null) {
                                invokeRouteMatch(req, res, badRequestRoute, true);
                            } else {
                                invokeExceptionHandlerIfPossible(req, res, e, HttpStatus.BAD_REQUEST);
                            }
                            return;
                        }
                    }
                }
                if (errorRoute != null) {
                    invokeRouteMatch(req, res, errorRoute, true);
                } else {
                    invokeExceptionHandlerIfPossible(req, res, e, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }
    }

    private void invokeExceptionHandlerIfPossible(
            HttpRequest<Object> req,
            MutableHttpResponse<Object> res,
            Throwable e,
            HttpStatus defaultStatus) {
        final ExceptionHandler<Throwable, ?> exceptionHandler = lookupExceptionHandler(e);
        if (exceptionHandler != null) {
            try {
                ServerRequestContext.with(req, () -> {
                    final Object result = exceptionHandler.handle(req, e);
                    if (result instanceof MutableHttpResponse) {
                        encodeResponse(AnnotationMetadata.EMPTY_METADATA, (MutableHttpResponse<Object>) result);
                    } else if (result != null) {
//                        final MutableHttpResponse<? super Object> response =
//                                exchange.getResponse().status(defaultStatus).body(result);
//                        encodeResponse(AnnotationMetadata.EMPTY_METADATA, response);
                    }
                });
            } catch (Throwable ex) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Error occurred executing exception handler [" + exceptionHandler.getClass() + "]: " + e.getMessage(), e);
                }
                emitError(res, req, (emitter) -> {
                    res.status(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                    emitter.onNext(res);
                    emitter.onComplete();
                });
            }
        } else {
            if (defaultStatus.getCode() >= 500) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(defaultStatus.getReason() + ": " + e.getMessage(), e);
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(defaultStatus.getReason() + ": " + e.getMessage(), e);
                }
            }
            emitError(res, req, (emitter) -> {
                res.status(defaultStatus)
                        .body(newJsonError(req, e.getMessage()));
                emitter.onNext(res);
                emitter.onComplete();
            });
        }
    }


    private void handlePageNotFound(MutableHttpResponse<Object> res, HttpRequest<Object> req) {
        handleStatusRoute(res, req, HttpStatus.NOT_FOUND);
    }

    private void handleStatusRoute(MutableHttpResponse<Object> res, HttpRequest<Object> req, HttpStatus httpStatus) {
        final RouteMatch<Object> notFoundRoute =
                router.route(httpStatus).orElse(null);

        if (notFoundRoute != null) {
            invokeRouteMatch(req, res, notFoundRoute, true);
        } else {
            emitError(res, req, emitter -> {
                res.status(httpStatus)
                        .body(newJsonError(req, httpStatus.getReason()));
                emitter.onNext(res);
                emitter.onComplete();
            });
        }
    }


    private void emitError(MutableHttpResponse<Object> res,
                           HttpRequest<Object> req,
                           FlowableOnSubscribe<MutableHttpResponse<?>> errorEmitter) {
        Publisher<? extends MutableHttpResponse<?>> responsePublisher = Flowable.create(
                errorEmitter, BackpressureStrategy.LATEST);

        responsePublisher = filterPublisher(new AtomicReference<>(req), responsePublisher, false);
        subscribeToResponsePublisher(
                req,
                res,
                null,
                true,
                responsePublisher,
                AnnotationMetadata.EMPTY_METADATA
        );
    }


    private JsonError newJsonError(HttpRequest<Object> req, String message) {
        JsonError jsonError = new JsonError(message);
        jsonError.link("self", req.getPath());
        return jsonError;
    }

}
