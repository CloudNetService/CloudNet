package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public abstract class DefaultMessenger implements CloudMessenger {

    @Override
    public @NotNull ITask<ChannelMessage> sendSingleChannelMessageQueryAsync(@NotNull ChannelMessage channelMessage) {
        CompletableTask<ChannelMessage> task = new CompletableTask<>();
        this.sendChannelMessageQueryAsync(channelMessage)
                .onComplete(channelMessages -> task.complete(channelMessages.isEmpty() ? null : channelMessages.iterator().next()))
                .onCancelled(v -> task.cancel(true))
                .addListener(ITaskListener.FIRE_EXCEPTION_ON_FAILURE);
        return task;
    }

    @Override
    public ChannelMessage sendSingleChannelMessageQuery(@NotNull ChannelMessage channelMessage) {
        Collection<ChannelMessage> messages = this.sendChannelMessageQuery(channelMessage);
        return messages.isEmpty() ? null : messages.iterator().next();
    }

    @Override
    public @NotNull Collection<ChannelMessage> sendChannelMessageQuery(@NotNull ChannelMessage channelMessage) {
        return this.sendChannelMessageQueryAsync(channelMessage).get(10, TimeUnit.SECONDS, Collections.emptyList());
    }

}
