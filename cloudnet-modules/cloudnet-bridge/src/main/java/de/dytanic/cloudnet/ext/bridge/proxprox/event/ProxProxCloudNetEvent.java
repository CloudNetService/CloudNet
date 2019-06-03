package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.proxprox.api.plugin.event.Event;

abstract class ProxProxCloudNetEvent extends Event {

    public final CloudNetDriver getDriver()
    {
        return CloudNetDriver.getInstance();
    }

    public final Wrapper getWrapper()
    {
        return Wrapper.getInstance();
    }

}