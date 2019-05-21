package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.event.Event;

abstract class GoMintCloudNetEvent extends Event {

    public final CloudNetDriver getDriver()
    {
        return CloudNetDriver.getInstance();
    }

    public final Wrapper getWrapper()
    {
        return Wrapper.getInstance();
    }

}