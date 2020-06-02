package de.dytanic.cloudnet.wrapper.provider;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.provider.DefaultMessenger;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class WrapperMessenger extends DefaultMessenger implements CloudMessenger {

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
                .onComplete(packet -> task.complete(packet.getBuffer().readableBytes() <= 1 ? Collections.emptyList() : packet.getBuffer().readObjectCollection(ChannelMessage.class)))
                .onCancelled(v -> task.cancel(true))
                .addListener(ITaskListener.FIRE_EXCEPTION_ON_FAILURE);
        return task;
    }

}
