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
