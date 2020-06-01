package de.dytanic.cloudnet.wrapper.provider;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class WrapperMessenger implements CloudMessenger {

    private final Wrapper wrapper;

    public WrapperMessenger(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public void sendChannelMessage(@NotNull ChannelMessage channelMessage) {
        this.wrapper.getNetworkClient().sendPacket(new PacketClientServerChannelMessage(channelMessage, false));
    }

    @Override
    public @NotNull ITask<Collection<ChannelMessage>> sendChannelMessageQueryAsync(@NotNull ChannelMessage channelMessage) {
        CompletableTask<Collection<ChannelMessage>> task = new CompletableTask<>();
        ITask<IPacket> packetTask = this.wrapper.getNetworkClient().getFirstChannel().sendQueryAsync(new PacketClientServerChannelMessage(channelMessage, true));
        packetTask
                .onComplete(packet -> task.complete(packet.getBody().readableBytes() <= 1 ? Collections.emptyList() : packet.getBody().readObjectCollection(ChannelMessage.class)))
                .onCancelled(v -> task.cancel(true))
                .addListener(ITaskListener.FIRE_EXCEPTION_ON_FAILURE);
        return task;
    }

    @Override
    public @NotNull Collection<ChannelMessage> sendChannelMessageQuery(@NotNull ChannelMessage channelMessage) {
        return this.sendChannelMessageQueryAsync(channelMessage).get(10, TimeUnit.SECONDS, Collections.emptyList());
    }
}
