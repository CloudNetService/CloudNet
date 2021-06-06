package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.IPacketSender;
import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * Includes the basic functions for the client and the server
 */
interface INetworkComponent extends IPacketSender {

  /**
   * Returns true if the network component allows to create a ssl connection
   */
  boolean isSslEnabled();

  /**
   * Returns all running enabled connections from the network component
   */
  Collection<INetworkChannel> getChannels();

  default INetworkChannel getFirstChannel() {
    Collection<INetworkChannel> channels = this.getChannels();
    return channels.isEmpty() ? null : channels.iterator().next();
  }

  Executor getPacketDispatcher();

  /**
   * Close all open connections from this network component
   */
  void closeChannels();

  /**
   * Returns the parent packet registry from all channels, that are this network component provide
   */
  IPacketListenerRegistry getPacketRegistry();
}
