package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import java.util.UUID;

/**
 * A packet represents the message format between a INetworkConnector and a
 * INetworkServer instance Every packet has a channel id, from that the
 * IPacketListenerRegistry, can filter its listeners for handle the IPacket
 * instance
 * <p>
 * The default implementation of the IPacket class is the Packet class in the
 * same package
 *
 * All packets require a channel, header and a body.
 * <p>
 * The channel id is the id from that the listeners should be filter The header
 * has the specify information or the data that is important The body has binary
 * packet information like for files, or zip compressed data
 *
 * @see Packet
 * @see INetworkClient
 * @see de.dytanic.cloudnet.driver.network.INetworkServer
 */
public interface IPacket {

  /**
   * Returns the uuid of the packet. Each packet should has a own defined UUID
   * instance, less the callback packets for synchronized messages between
   * client and server
   *
   * @return the own uniqueId
   */
  UUID getUniqueId();

  /**
   * Returns the channel id, in that the IPacketListenerRegistry class should
   * the listeners work with that packet
   *
   * @return the channel id in that the packet is defined
   */
  int getChannel();

  /**
   * The header, contain all important information from the packet, to specify
   * the packet type as channel does and/or can has the data, which that are
   * important to handle with
   * <p>
   * The encoding should be UTF-8
   *
   * @return the header as JsonDocument instance from this packet
   */
  JsonDocument getHeader();

  /**
   * Returns the packet body, for transport for extra data, like files or zip
   * archives or something else
   * <p>
   * The max length of the body can only be (Integer.MAX_VALUE - 1)
   *
   * @return the body as byte array in bytes
   */
  byte[] getBody();
}