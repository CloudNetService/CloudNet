package de.dytanic.cloudnet.driver.network.http;

import de.dytanic.cloudnet.driver.network.HostAndPort;

public interface HttpChannel extends AutoCloseable {

    HostAndPort serverAddress();

    HostAndPort clientAddress();

}