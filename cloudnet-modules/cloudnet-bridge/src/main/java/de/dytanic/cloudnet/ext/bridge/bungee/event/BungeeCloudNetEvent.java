package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.plugin.Event;

abstract class BungeeCloudNetEvent extends Event {

    public final CloudNetDriver getDriver() {
        return CloudNetDriver.getInstance();
    }

    public final Wrapper getWrapper() {
        return Wrapper.getInstance();
    }

}