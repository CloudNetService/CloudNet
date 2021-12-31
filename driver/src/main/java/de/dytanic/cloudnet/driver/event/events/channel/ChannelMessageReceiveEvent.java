/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.event.events.channel;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * This event is being called whenever a channel message is received. You can send channel messages with the methods in
 * {@link CloudNetDriver#messenger()}.
 */
public final class ChannelMessageReceiveEvent extends NetworkEvent {

  private final ChannelMessage channelMessage;
  private final boolean query;

  private ChannelMessage queryResponse;

  public ChannelMessageReceiveEvent(
    @NonNull ChannelMessage message,
    @NonNull NetworkChannel networkChannel,
    boolean query
  ) {
    super(networkChannel);

    this.channelMessage = message;
    this.query = query;
  }

  public @NonNull ChannelMessageSender sender() {
    return this.channelMessage.sender();
  }

  public @NonNull Collection<ChannelMessageTarget> targets() {
    return this.channelMessage.targets();
  }

  public @NonNull String channel() {
    return this.channelMessage.channel();
  }

  public @NonNull String message() {
    return this.channelMessage.message();
  }

  public @NonNull ChannelMessage channelMessage() {
    return this.channelMessage;
  }

  public @NonNull DataBuf content() {
    return this.channelMessage.content();
  }

  public boolean query() {
    return this.query;
  }

  public void binaryResponse(@NonNull DataBuf dataBuf) {
    this.queryResponse(ChannelMessage.buildResponseFor(this.channelMessage).buffer(dataBuf).build());
  }

  public @Nullable ChannelMessage queryResponse() {
    return this.queryResponse;
  }

  public void queryResponse(@Nullable ChannelMessage queryResponse) {
    Preconditions.checkArgument(this.query, "Cannot set query response of no query");
    this.queryResponse = queryResponse;
  }
}
