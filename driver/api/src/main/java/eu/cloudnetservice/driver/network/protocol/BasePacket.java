/*
 * Copyright 2019-2024 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.driver.network.protocol;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of a packet. Each subclass of a packet might implement the packet interface themselves,
 * but it is much more convenient and easy to extend from this class.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public class BasePacket implements Packet {

  protected final int channel;
  protected final DataBuf dataBuf;
  protected final boolean prioritized;
  protected final Instant creationStamp;

  protected UUID uniqueId;

  /**
   * Constructs a new base packet instance.
   *
   * @param channel the channel to which the packet was sent.
   * @param dataBuf the buffer (or content) of the packet.
   * @throws NullPointerException if the given buffer is null.
   */
  public BasePacket(int channel, @NonNull DataBuf dataBuf) {
    this(channel, false, dataBuf);
  }

  /**
   * Constructs a new base packet instance.
   *
   * @param channel     the channel to which the packet was sent.
   * @param prioritized if the packet should be prioritized.
   * @param dataBuf     the buffer (or content) of the packet.
   * @throws NullPointerException if the given buffer is null.
   */
  public BasePacket(int channel, boolean prioritized, @NonNull DataBuf dataBuf) {
    this.channel = channel;
    this.dataBuf = dataBuf;
    this.prioritized = prioritized;
    this.creationStamp = Instant.now();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Packet constructResponse(@NonNull DataBuf content) {
    var packet = new BasePacket(-1, content);
    packet.uniqueId(this.uniqueId());
    return packet;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable UUID uniqueId() {
    return this.uniqueId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void uniqueId(@Nullable UUID uniqueId) {
    this.uniqueId = uniqueId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int channel() {
    return this.channel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean prioritized() {
    return this.prioritized;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean readable() {
    return this.dataBuf.accessible() && this.dataBuf.readableBytes() > 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf content() {
    return this.dataBuf;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Instant creation() {
    return this.creationStamp;
  }
}
