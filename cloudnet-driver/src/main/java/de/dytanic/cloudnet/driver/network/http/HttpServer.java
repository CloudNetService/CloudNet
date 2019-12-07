package de.dytanic.cloudnet.driver.network.http;

import de.dytanic.cloudnet.driver.network.HostAndPort;

public interface HttpServer extends HttpComponent<HttpServer> {

    boolean addListener(int port);

    boolean addListener(HostAndPort hostAndPort);

}