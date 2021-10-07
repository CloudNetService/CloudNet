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
