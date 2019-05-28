package de.dytanic.cloudnet.driver.network.http;

import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import java.util.Collection;

public interface IHttpContext {

  IWebSocketChannel upgrade();

  IWebSocketChannel webSocketChanel();

  IHttpChannel channel();

  IHttpRequest request();

  IHttpResponse response();

  IHttpHandler peekLast();

  boolean cancelNext();

  IHttpComponent<?> component();

  IHttpContext closeAfter(boolean value);

  boolean closeAfter();

  HttpCookie cookie(String name);

  Collection<HttpCookie> cookies();

  boolean hasCookie(String name);

  IHttpContext setCookies(Collection<HttpCookie> cookies);

  IHttpContext addCookie(HttpCookie httpCookie);

  IHttpContext removeCookie(String name);

  IHttpContext clearCookies();

}