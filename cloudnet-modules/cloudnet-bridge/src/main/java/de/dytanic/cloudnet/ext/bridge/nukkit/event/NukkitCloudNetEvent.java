package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.Server;
import cn.nukkit.event.Event;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;

/**
 * All CloudNet events will mostly fire asynchronously, depending on how they were triggered.
 * Check {@link Server#isPrimaryThread()} and treat the event appropriately.
 */
abstract class NukkitCloudNetEvent extends Event {

    public final CloudNetDriver getDriver() {
        return CloudNetDriver.getInstance();
    }

    public final Wrapper getWrapper() {
        return Wrapper.getInstance();
    }

}