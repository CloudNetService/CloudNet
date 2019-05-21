package de.dytanic.cloudnet.driver.network.http;

import de.dytanic.cloudnet.driver.network.HostAndPort;

public interface IHttpServer extends IHttpComponent<IHttpServer> {

    boolean addListener(int port);

    boolean addListener(HostAndPort hostAndPort);

}