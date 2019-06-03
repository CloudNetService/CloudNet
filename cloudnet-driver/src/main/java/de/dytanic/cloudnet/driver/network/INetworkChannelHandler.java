package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.driver.network.protocol.Packet;

/**
 * A networkChannelHandler provides the operation with the INetworkChannel
 *
 * @see INetworkChannel
 */
public interface INetworkChannelHandler {

  /**
   * Handles an new open connected channel
   *
   * @param channel the providing channel on that this handler is sets on this
   */
  void handleChannelInitialize(INetworkChannel channel) throws Exception;

  /**
   * Handles a incoming packet from a provided channel, that contains that
   * channel handler
   *
   * @param channel the providing channel on that this handler is sets on this
   * @param packet the packet, that will received from the remote component
   * @return should return true that, the packet that was received is allowed to
   * handle from the packet listeners at the packetListenerRegistry
   */
  boolean handlePacketReceive(INetworkChannel channel, Packet packet)
      throws Exception;

  /**
   * Handles the close phase from a NetworkChannel
   *
   * @param channel the providing channel on that this handler is sets on this
   */
  void handleChannelClose(INetworkChannel channel) throws Exception;
}