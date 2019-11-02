package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.network.http.HttpVersion;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.charset.StandardCharsets;
import java.util.Map;

final class NettyHttpServerResponse implements IHttpResponse {

    protected final NettyHttpServerContext context;

    protected final DefaultFullHttpResponse httpResponse;

    public NettyHttpServerResponse(NettyHttpServerContext context, HttpRequest httpRequest) {
        this.context = context;
        this.httpResponse = new DefaultFullHttpResponse(
                httpRequest.protocolVersion(),
                HttpResponseStatus.NOT_FOUND,
                Unpooled.buffer()
        );
    }

    @Override
    public int statusCode() {
        return this.httpResponse.status().code();
    }

    @Override
    public IHttpResponse statusCode(int code) {
        this.httpResponse.setStatus(HttpResponseStatus.valueOf(code));
        return this;
    }

    @Override
    public IHttpContext context() {
        return this.context;
    }

    @Override
    public String header(String name) {
        Validate.checkNotNull(name);
        return this.httpResponse.headers().getAsString(name);
    }

    @Override
    public int headerAsInt(String name) {
        Validate.checkNotNull(name);
        return this.httpResponse.headers().getInt(name);
    }

    @Override
    public boolean headerAsBoolean(String name) {
        Validate.checkNotNull(name);
        return Boolean.parseBoolean(this.httpResponse.headers().get(name));
    }

    @Override
    public IHttpResponse header(String name, String value) {
        Validate.checkNotNull(name);
        Validate.checkNotNull(value);

        this.httpResponse.headers().set(name, value);
        return this;
    }

    @Override
    public IHttpResponse removeHeader(String name) {
        Validate.checkNotNull(name);
        this.httpResponse.headers().remove(name);
        return this;
    }

    @Override
    public IHttpResponse clearHeaders() {
        this.httpResponse.headers().clear();
        return this;
    }

    @Override
    public boolean hasHeader(String name) {
        Validate.checkNotNull(name);
        return this.httpResponse.headers().contains(name);
    }

    @Override
    public Map<String, String> headers() {
        Map<String, String> maps = Maps.newHashMap(this.httpResponse.headers().size());

        for (String key : this.httpResponse.headers().names()) {
            maps.put(key, this.httpResponse.headers().get(key));
        }

        return maps;
    }

    @Override
    public HttpVersion version() {
        return this.getCloudNetHttpVersion(this.httpResponse.protocolVersion());
    }

    @Override
    public IHttpResponse version(HttpVersion version) {
        Validate.checkNotNull(version);

        this.httpResponse.setProtocolVersion(this.getNettyHttpVersion(version));
        return this;
    }

    @Override
    public byte[] body() {
        return this.httpResponse.content().array();
    }

    @Override
    public String bodyAsString() {
        return new String(body(), StandardCharsets.UTF_8);
    }

    @Override
    public IHttpResponse body(byte[] byteArray) {
        Validate.checkNotNull(byteArray);

        this.httpResponse.content().clear();
        this.httpResponse.content().writeBytes(byteArray);
        return this;
    }

    @Override
    public IHttpResponse body(String text) {
        Validate.checkNotNull(text);

        this.httpResponse.content().clear();
        this.httpResponse.content().writeBytes(text.getBytes(StandardCharsets.UTF_8));
        return this;
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