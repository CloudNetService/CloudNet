package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.network.http.*;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketChannel;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

final class NettyHttpServerContext implements HttpContext {

    protected final Collection<HttpCookie> cookies = Iterables.newArrayList();

    protected final Channel nettyChannel;

    protected final NettyHttpServer nettyHttpServer;

    protected final NettyHttpChannel channel;

    protected final io.netty.handler.codec.http.HttpRequest httpRequest;

    protected final NettyHttpServerRequest httpServerRequest;

    protected final NettyHttpServerResponse httpServerResponse;

    protected volatile boolean closeAfter = true, cancelNext = false, cancelSendResponse = false;

    protected volatile NettyWebSocketServerChannel webSocketServerChannel;

    protected HttpHandler lastHandler;

    public NettyHttpServerContext(NettyHttpServer nettyHttpServer, NettyHttpChannel channel, URI uri, Map<String, String> pathParameters, io.netty.handler.codec.http.HttpRequest httpRequest) {
        this.nettyHttpServer = nettyHttpServer;
        this.channel = channel;
        this.httpRequest = httpRequest;
        this.nettyChannel = channel.getChannel();

        this.httpServerRequest = new NettyHttpServerRequest(this, httpRequest, pathParameters, uri);
        this.httpServerResponse = new NettyHttpServerResponse(this, httpRequest);

        if (this.httpRequest.headers().contains("Cookie")) {
            this.cookies.addAll(Iterables.map(ServerCookieDecoder.LAX.decode(this.httpRequest.headers().get("Cookie")), cookie -> new HttpCookie(
                    cookie.name(),
                    cookie.value(),
                    cookie.domain(),
                    cookie.path(),
                    cookie.maxAge()
            )));
        }

        this.updateHeaderResponse();
    }

    @Override
    public WebSocketChannel upgrade() {
        if (webSocketServerChannel == null) {
            cancelSendResponse = true;
            WebSocketServerHandshakerFactory webSocketServerHandshakerFactory = new WebSocketServerHandshakerFactory(
                    httpRequest.uri(),
                    null,
                    false
            );

            nettyChannel.pipeline().remove("http-server-handler");

            WebSocketServerHandshaker webSocketServerHandshaker = webSocketServerHandshakerFactory.newHandshaker(httpRequest);
            webSocketServerHandshaker.handshake(nettyChannel, httpRequest);

            webSocketServerChannel = new NettyWebSocketServerChannel(channel, nettyChannel, webSocketServerHandshaker);
            nettyChannel.pipeline().addLast("websocket-server-channel-handler", new NettyWebSocketServerChannelHandler(webSocketServerChannel));

            closeAfter(false);
        }

        return webSocketServerChannel;
    }

    @Override
    public WebSocketChannel webSocketChanel() {
        return webSocketServerChannel;
    }

    @Override
    public HttpChannel channel() {
        return this.channel;
    }

    @Override
    public HttpRequest request() {
        return this.httpServerRequest;
    }

    @Override
    public HttpResponse response() {
        return this.httpServerResponse;
    }

    @Override
    public boolean cancelNext() {
        return this.cancelNext = true;
    }

    @Override
    public HttpHandler peekLast() {
        return this.lastHandler;
    }

    @Override
    public HttpComponent<HttpServer> component() {
        return this.nettyHttpServer;
    }

    @Override
    public HttpContext closeAfter(boolean value) {
        this.closeAfter = value;
        return this;
    }

    @Override
    public boolean closeAfter() {
        return closeAfter;
    }

    @Override
    public HttpCookie cookie(String name) {
        Validate.checkNotNull(name);

        return Iterables.first(this.cookies, httpCookie -> httpCookie.getName().equalsIgnoreCase(name));
    }

    @Override
    public Collection<HttpCookie> cookies() {
        return this.cookies;
    }

    @Override
    public boolean hasCookie(String name) {
        Validate.checkNotNull(name);

        return Iterables.first(this.cookies, httpCookie -> httpCookie.getName().equalsIgnoreCase(name)) != null;
    }

    @Override
    public HttpContext setCookies(Collection<HttpCookie> cookies) {
        Validate.checkNotNull(cookies);

        this.cookies.clear();
        this.cookies.addAll(cookies);
        this.updateHeaderResponse();

        return this;
    }

    @Override
    public HttpContext addCookie(HttpCookie httpCookie) {
        Validate.checkNotNull(httpCookie);

        HttpCookie cookie = cookie(httpCookie.getName());

        if (cookie != null) {
            this.removeCookie(cookie.getName());
        }
        this.cookies.add(httpCookie);
        this.updateHeaderResponse();

        return this;
    }

    @Override
    public HttpContext removeCookie(String name) {
        Validate.checkNotNull(name);

        HttpCookie cookie = cookie(name);
        if (cookie != null) {
            cookie.setMaxAge(-1);
        }

        this.updateHeaderResponse();
        return this;
    }

    @Override
    public HttpContext clearCookies() {
        this.cookies.clear();
        this.updateHeaderResponse();
        return this;
    }

    private void updateHeaderResponse() {
        if (cookies.isEmpty()) {
            this.httpServerResponse.httpResponse.headers().remove("Set-Cookie");
        } else {
            this.httpServerResponse.httpResponse.headers()
                    .set("Set-Cookie", ServerCookieEncoder.LAX.encode(Iterables.map(this.cookies, httpCookie -> {
                        Cookie cookie = new DefaultCookie(httpCookie.getName(), httpCookie.getValue());
                        cookie.setDomain(httpCookie.getDomain());
                        cookie.setMaxAge(httpCookie.getMaxAge());
                        cookie.setPath(httpCookie.getPath());

                        return cookie;
                    })));
        }
    }

    public void setLastHandler(HttpHandler lastHandler) {
        this.lastHandler = lastHandler;
    }
}