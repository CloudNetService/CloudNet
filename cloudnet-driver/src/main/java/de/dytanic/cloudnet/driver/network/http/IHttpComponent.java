package de.dytanic.cloudnet.driver.network.http;

import java.util.Collection;

public interface IHttpComponent<T extends IHttpComponent> extends AutoCloseable {

    boolean isSslEnabled();

    T registerHandler(String path, IHttpHandler... handlers);

    T registerHandler(String path, int priority, IHttpHandler... handlers);

    T registerHandler(String path, Integer port, int priority, IHttpHandler... handlers);

    T removeHandler(IHttpHandler handler);

    T removeHandler(Class<? extends IHttpHandler> handler);

    T removeHandler(ClassLoader classLoader);

    Collection<IHttpHandler> getHttpHandlers();

    T clearHandlers();

}