package de.dytanic.cloudnet.driver.network.http;

import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketChannel;

import java.util.Collection;

public interface HttpContext {

    WebSocketChannel upgrade();

    WebSocketChannel webSocketChanel();

    HttpChannel channel();

    HttpRequest request();

    HttpResponse response();

    HttpHandler peekLast();

    boolean cancelNext();

    HttpComponent<?> component();

    HttpContext closeAfter(boolean value);

    boolean closeAfter();

    HttpCookie cookie(String name);

    Collection<HttpCookie> cookies();

    boolean hasCookie(String name);

    HttpContext setCookies(Collection<HttpCookie> cookies);

    HttpContext addCookie(HttpCookie httpCookie);

    HttpContext removeCookie(String name);

    HttpContext clearCookies();

}