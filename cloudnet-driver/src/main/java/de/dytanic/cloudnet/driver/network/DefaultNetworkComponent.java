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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public interface DefaultNetworkComponent extends INetworkComponent {

  Collection<INetworkChannel> getModifiableChannels();

  @Override
  default void closeChannels() {
    for (INetworkChannel channel : this.getModifiableChannels()) {
      try {
        channel.close();
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    }

    this.getModifiableChannels().clear();
  }

  @Override
  default void sendPacket(@NotNull IPacket packet) {
    Preconditions.checkNotNull(packet);

    for (INetworkChannel channel : this.getModifiableChannels()) {
      channel.sendPacket(packet);
    }
  }

  @Override
  default void sendPacketSync(@NotNull IPacket packet) {
    Preconditions.checkNotNull(packet);

    for (INetworkChannel channel : this.getModifiableChannels()) {
      channel.sendPacketSync(packet);
    }
  }

  @Override
  default void sendPacket(@NotNull IPacket... packets) {
    Preconditions.checkNotNull(packets);

    for (INetworkChannel channel : this.getModifiableChannels()) {
      channel.sendPacket(packets);
    }
  }

}
