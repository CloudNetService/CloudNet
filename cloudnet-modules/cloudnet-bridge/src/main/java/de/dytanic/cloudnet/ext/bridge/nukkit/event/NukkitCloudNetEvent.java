package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.Event;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;

abstract class NukkitCloudNetEvent extends Event {

    public final CloudNetDriver getDriver() {
        return CloudNetDriver.getInstance();
    }

    public final Wrapper getWrapper() {
        return Wrapper.getInstance();
    }

}