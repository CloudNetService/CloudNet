package de.dytanic.cloudnet.wrapper.provider;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;

public class WrapperMessenger implements CloudMessenger {

    private final Wrapper wrapper;

    public WrapperMessenger(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public void sendChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data) {
        Preconditions.checkNotNull(channel);
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(data);

        this.wrapper.getNetworkClient().sendPacket(new PacketClientServerChannelMessage(channel, message, data));
    }

    @Override
    public void sendChannelMessage(@NotNull ServiceInfoSnapshot targetServiceInfoSnapshot, @NotNull String channel, @NotNull String message, @NotNull JsonDocument data) {
        Preconditions.checkNotNull(targetServiceInfoSnapshot);
        Preconditions.checkNotNull(channel);
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(data);

        this.wrapper.getNetworkClient().sendPacket(new PacketClientServerChannelMessage(targetServiceInfoSnapshot.getServiceId().getUniqueId(), channel, message, data));
    }

    @Override
    public void sendChannelMessage(@NotNull ServiceTask targetServiceTask, @NotNull String channel, @NotNull String message, @NotNull JsonDocument data) {
        Preconditions.checkNotNull(targetServiceTask);
        Preconditions.checkNotNull(channel);
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(data);

        this.wrapper.getNetworkClient().sendPacket(new PacketClientServerChannelMessage(targetServiceTask.getName(), channel, message, data));
    }

    @Override
    public void sendChannelMessage(@NotNull ServiceEnvironmentType targetEnvironment, @NotNull String channel, @NotNull String message, @NotNull JsonDocument data) {
        Preconditions.checkNotNull(targetEnvironment);
        Preconditions.checkNotNull(channel);
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(data);

        this.wrapper.getNetworkClient().sendPacket(new PacketClientServerChannelMessage(targetEnvironment, channel, message, data));
    }
}
