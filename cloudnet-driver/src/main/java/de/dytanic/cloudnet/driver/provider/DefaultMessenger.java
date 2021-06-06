package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public abstract class DefaultMessenger implements CloudMessenger {

  @Override
  public @NotNull ITask<ChannelMessage> sendSingleChannelMessageQueryAsync(@NotNull ChannelMessage channelMessage) {
    return this.sendChannelMessageQueryAsync(channelMessage)
      .map(messages -> messages.isEmpty() ? null : messages.iterator().next());
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
