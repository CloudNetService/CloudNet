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

package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public interface DefaultNetworkComponent extends INetworkComponent {

  Logger LOGGER = LogManager.logger(DefaultNetworkComponent.class);

  Collection<INetworkChannel> modifiableChannels();

  @Override
  default void closeChannels() {
    for (var channel : this.modifiableChannels()) {
      try {
        channel.close();
      } catch (Exception exception) {
        LOGGER.severe("Exception while closing channels", exception);
      }
    }

    this.modifiableChannels().clear();
  }

  @Override
  default void sendPacket(@NotNull IPacket packet) {
    for (var channel : this.modifiableChannels()) {
      channel.sendPacket(packet);
    }
  }

  @Override
  default void sendPacketSync(@NotNull IPacket packet) {
    for (var channel : this.modifiableChannels()) {
      channel.sendPacketSync(packet);
    }
  }

  @Override
  default void sendPacket(@NotNull IPacket... packets) {
    for (var channel : this.modifiableChannels()) {
      channel.sendPacket(packets);
    }
  }
}
