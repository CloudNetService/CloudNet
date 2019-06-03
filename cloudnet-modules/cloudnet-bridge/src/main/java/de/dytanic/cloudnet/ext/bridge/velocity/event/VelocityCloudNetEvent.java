package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;

abstract class VelocityCloudNetEvent {

    public final CloudNetDriver getDriver()
    {
        return CloudNetDriver.getInstance();
    }

    public final Wrapper getWrapper()
    {
        return Wrapper.getInstance();
    }

}