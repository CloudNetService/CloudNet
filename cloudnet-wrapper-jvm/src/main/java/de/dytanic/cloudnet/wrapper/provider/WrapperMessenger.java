package de.dytanic.cloudnet.wrapper.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
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
        throw new UnsupportedOperationException("not implemented yet"); // TODO
    }

    @Override
    public @NotNull Collection<ChannelMessage> sendChannelMessageQuery(@NotNull ChannelMessage channelMessage) {
        return this.sendChannelMessageQueryAsync(channelMessage).get(10, TimeUnit.SECONDS, Collections.emptyList());
    }
}
