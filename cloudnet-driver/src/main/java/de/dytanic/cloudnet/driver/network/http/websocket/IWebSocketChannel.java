package de.dytanic.cloudnet.driver.network.http.websocket;

import de.dytanic.cloudnet.driver.network.http.IHttpChannel;
import java.util.Collection;

public interface IWebSocketChannel extends AutoCloseable {

  IWebSocketChannel addListener(IWebSocketListener... listeners);

  IWebSocketChannel removeListener(IWebSocketListener... listeners);

  IWebSocketChannel removeListener(
      Collection<Class<? extends IWebSocketListener>> classes);

  IWebSocketChannel removeListener(ClassLoader classLoader);

  IWebSocketChannel clearListeners();

  Collection<IWebSocketListener> getListeners();

  IWebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType,
      String text);

  IWebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType,
      byte[] bytes);

  void close(int statusCode, String reasonText);

  IHttpChannel channel();

}