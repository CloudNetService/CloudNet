package de.dytanic.cloudnet.driver.network.http;

import java.util.Collection;

public interface HttpComponent<T extends HttpComponent> extends AutoCloseable {

    boolean isSslEnabled();

    T registerHandler(String path, HttpHandler... handlers);

    T registerHandler(String path, int priority, HttpHandler... handlers);

    T registerHandler(String path, Integer port, int priority, HttpHandler... handlers);

    T removeHandler(HttpHandler handler);

    T removeHandler(Class<? extends HttpHandler> handler);

    T removeHandler(ClassLoader classLoader);

    Collection<HttpHandler> getHttpHandlers();

    T clearHandlers();

}