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

package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;

public interface ClusterNodeServer extends NodeServer, AutoCloseable {

  @Override
  @NonNull ClusterNodeServerProvider provider();

  @UnknownNullability NetworkChannel channel();

  void channel(@NonNull NetworkChannel channel);

  boolean connected();

  void saveSendPacket(@NonNull Packet packet);

  void saveSendPacketSync(@NonNull Packet packet);

  boolean acceptableConnection(@NonNull NetworkChannel channel, @NonNull String nodeId);

  void syncClusterData(boolean force);

  void shutdown();

  @Override
  default boolean available() {
    return this.channel() != null && this.nodeInfoSnapshot() != null;
  }
}
