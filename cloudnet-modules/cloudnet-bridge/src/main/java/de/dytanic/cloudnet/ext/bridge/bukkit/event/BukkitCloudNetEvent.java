package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.event.Event;

abstract class BukkitCloudNetEvent extends Event {

  public final CloudNetDriver getDriver() {
    return CloudNetDriver.getInstance();
  }

  public final Wrapper getWrapper() {
    return Wrapper.getInstance();
  }

}