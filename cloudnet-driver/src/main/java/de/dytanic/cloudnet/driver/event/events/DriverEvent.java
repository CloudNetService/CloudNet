package de.dytanic.cloudnet.driver.event.events;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;

public abstract class DriverEvent extends Event {

  public CloudNetDriver getDriver() {
    return CloudNetDriver.getInstance();
  }

}
