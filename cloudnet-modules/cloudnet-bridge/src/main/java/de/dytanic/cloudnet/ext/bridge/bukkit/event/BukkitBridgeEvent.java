package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/**
 * All CloudNet events will mostly fire asynchronously, depending on how they were triggered. Check {@link
 * Event#isAsynchronous()} and treat the event appropriately.
 */
abstract class BukkitBridgeEvent extends Event {

  public BukkitBridgeEvent() {
    super(!Bukkit.isPrimaryThread());
  }

}
