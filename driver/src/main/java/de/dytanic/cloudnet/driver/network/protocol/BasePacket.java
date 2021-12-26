/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * The default simple implementation of the IPacket interface. You can create with the constructor all new packets or
 * use the class as superclass from an another for specific constructor usage.
 * <p>
 * All packets require a channel, header and a body.
 * <p>
 * The channel id is the id from that the listeners should be filter The header has the specify information or the data
 * that is important The body has binary packet information like for files, or zip compressed data
 */
@ToString
@EqualsAndHashCode
public class BasePacket implements Packet {

  public static final Packet EMPTY = new BasePacket(-1, null);

  protected final int channel;
  protected final DataBuf dataBuf;
  protected final long creationMillis;

  protected UUID uniqueId;

  public BasePacket() {
    this(0, null);
  }

  public BasePacket(int channel, DataBuf dataBuf) {
    this.channel = channel;
    this.dataBuf = dataBuf;
    this.creationMillis = System.currentTimeMillis();
  }

  @Override
  public @NonNull Packet constructResponse(@NonNull DataBuf content) {
    var packet = new BasePacket(-1, content);
    packet.uniqueId(this.uniqueId());
    return packet;
  }

  @Override
  public @Nullable UUID uniqueId() {
    return this.uniqueId;
  }

  @Override
  public void uniqueId(@Nullable UUID uniqueId) {
    this.uniqueId = uniqueId;
  }

  @Override
  public int channel() {
    return this.channel;
  }

  @Override
  public @NonNull DataBuf content() {
    return this.dataBuf;
  }

  @Override
  public long creationMillis() {
    return this.creationMillis;
  }
}
