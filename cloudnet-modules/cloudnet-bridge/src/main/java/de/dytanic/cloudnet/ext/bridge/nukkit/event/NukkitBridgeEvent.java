package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.Server;
import cn.nukkit.event.Event;

/**
 * All CloudNet events will mostly fire asynchronously, depending on how they were triggered.
 * Check {@link Server#isPrimaryThread()} and treat the event appropriately.
 */
abstract class NukkitBridgeEvent extends Event {
}