package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.network.http.HttpVersion;
import de.dytanic.cloudnet.driver.network.http.HttpContext;
import de.dytanic.cloudnet.driver.network.http.HttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

final class NettyHttpServerRequest implements HttpRequest {

    protected final NettyHttpServerContext context;

    protected final io.netty.handler.codec.http.HttpRequest httpRequest;

    protected final URI uri;

    protected final Map<String, String> pathParameters;

    protected final Map<String, List<String>> queryParameters;

    protected byte[] body;

    public NettyHttpServerRequest(NettyHttpServerContext context, io.netty.handler.codec.http.HttpRequest httpRequest, Map<String, String> pathParameters, URI uri) {
        this.context = context;
        this.httpRequest = httpRequest;
        this.uri = uri;
        this.pathParameters = pathParameters;
        this.queryParameters = new QueryStringDecoder(httpRequest.uri()).parameters();
    }

    @Override
    public Map<String, String> pathParameters() {
        return this.pathParameters;
    }

    @Override
    public String path() {
        return this.uri.getPath();
    }

    @Override
    public String uri() {
        return httpRequest.uri();
    }

    @Override
    public String method() {
        return this.httpRequest.method().name();
    }

    @Override
    public Map<String, List<String>> queryParameters() {
        return this.queryParameters;
    }

    @Override
    public HttpContext context() {
        return this.context;
    }

    @Override
    public String header(String name) {
        Validate.checkNotNull(name);
        return this.httpRequest.headers().getAsString(name);
    }

    @Override
    public int headerAsInt(String name) {
        Validate.checkNotNull(name);
        return this.httpRequest.headers().getInt(name);
    }

    @Override
    public boolean headerAsBoolean(String name) {
        Validate.checkNotNull(name);
        return Boolean.parseBoolean(this.httpRequest.headers().get(name));
    }

    @Override
    public HttpRequest header(String name, String value) {
        Validate.checkNotNull(name);
        Validate.checkNotNull(value);

        this.httpRequest.headers().set(name, value);
        return this;
    }

    @Override
    public HttpRequest removeHeader(String name) {
        Validate.checkNotNull(name);
        this.httpRequest.headers().remove(name);
        return this;
    }

    @Override
    public HttpRequest clearHeaders() {
        this.httpRequest.headers().clear();
        return this;
    }

    @Override
    public boolean hasHeader(String name) {
        Validate.checkNotNull(name);
        return this.httpRequest.headers().contains(name);
    }

    @Override
    public Map<String, String> headers() {
        Map<String, String> maps = Maps.newHashMap(this.httpRequest.headers().size());

        for (String key : this.httpRequest.headers().names()) {
            maps.put(key, this.httpRequest.headers().get(key));
        }

        return maps;
    }

    @Override
    public HttpVersion version() {
        return this.getCloudNetHttpVersion(this.httpRequest.protocolVersion());
    }

    @Override
    public HttpRequest version(HttpVersion version) {
        Validate.checkNotNull(version);

        this.httpRequest.setProtocolVersion(this.getNettyHttpVersion(version));
        return this;
    }

    @Override
    public byte[] body() {
        if (httpRequest instanceof FullHttpRequest) {
            if (body == null) {
                FullHttpRequest httpRequest = (FullHttpRequest) this.httpRequest;

                int length = httpRequest.content().readableBytes();

                if (httpRequest.content().hasArray()) {
                    body = httpRequest.content().array();
                } else {
                    body = new byte[length];
                    httpRequest.content().getBytes(httpRequest.content().readerIndex(), body);
                }
            }

            return body;
        }

        return new byte[0];
    }

    @Override
    public String bodyAsString() {
        return new String(body(), StandardCharsets.UTF_8);
    }

    @Override
    public HttpRequest body(byte[] byteArray) {
        throw new UnsupportedOperationException("No setting http body in request message by client");
    }

    @Override
    public HttpRequest body(String text) {
        Validate.checkNotNull(text);

        return this.body(text.getBytes(StandardCharsets.UTF_8));
    }

    private HttpVersion getCloudNetHttpVersion(io.netty.handler.codec.http.HttpVersion httpVersion) {
        if (httpVersion == io.netty.handler.codec.http.HttpVersion.HTTP_1_0) {
            return HttpVersion.HTTP_1_0;
        }

        if (httpVersion == io.netty.handler.codec.http.HttpVersion.HTTP_1_1) {
            return HttpVersion.HTTP_1_1;
        }

        return HttpVersion.HTTP_1_0;
    }

    private io.netty.handler.codec.http.HttpVersion getNettyHttpVersion(HttpVersion httpVersion) {
        if (httpVersion == HttpVersion.HTTP_1_0) {
            return io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
        }

        if (httpVersion == HttpVersion.HTTP_1_1) {
            return io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
        }

        return io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
    }

}