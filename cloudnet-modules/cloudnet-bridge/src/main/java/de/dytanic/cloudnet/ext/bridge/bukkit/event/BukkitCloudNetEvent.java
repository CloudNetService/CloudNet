package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/**
 * All CloudNet events will mostly fire asynchronously, depending on how they were triggered. Check {@link
 * Event#isAsynchronous()} and treat the event appropriately.
 */
abstract class BukkitCloudNetEvent extends Event {

  public BukkitCloudNetEvent() {
    super(!Bukkit.isPrimaryThread());
  }

  public final CloudNetDriver getDriver() {
    return CloudNetDriver.getInstance();
  }

  public final Wrapper getWrapper() {
    return Wrapper.getInstance();
  }

}
