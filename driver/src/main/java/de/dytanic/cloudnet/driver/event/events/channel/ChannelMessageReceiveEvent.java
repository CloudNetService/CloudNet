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

package de.dytanic.cloudnet.driver.event.events.channel;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This event is being called whenever a channel message is received. You can send channel messages with the methods in
 * {@link CloudNetDriver#getMessenger()}.
 */
public final class ChannelMessageReceiveEvent extends NetworkEvent {

  private final ChannelMessage channelMessage;
  private final boolean query;

  private ChannelMessage queryResponse;

  public ChannelMessageReceiveEvent(
    @NotNull ChannelMessage message,
    @NotNull INetworkChannel networkChannel,
    boolean query
  ) {
    super(networkChannel);

    this.channelMessage = message;
    this.query = query;
  }

  @NotNull
  public ChannelMessageSender getSender() {
    return this.channelMessage.getSender();
  }

  @NotNull
  public Collection<ChannelMessageTarget> getTargets() {
    return this.channelMessage.getTargets();
  }

  @NotNull
  public String getChannel() {
    return this.channelMessage.getChannel();
  }

  @Nullable
  public String getMessage() {
    return this.channelMessage.getMessage();
  }

  @NotNull
  public ChannelMessage getChannelMessage() {
    return this.channelMessage;
  }

  @NotNull
  public DataBuf getContent() {
    return this.channelMessage.getContent();
  }

  public boolean isQuery() {
    return this.query;
  }

  public void setBinaryResponse(@NotNull DataBuf dataBuf) {
    this.setQueryResponse(ChannelMessage.buildResponseFor(this.channelMessage).buffer(dataBuf).build());
  }

  public DataBuf.Mutable response() {
    DataBuf.Mutable dataBuf = DataBuf.empty();
    this.setBinaryResponse(dataBuf);
    return dataBuf;
  }

  @Nullable
  public ChannelMessage getQueryResponse() {
    return this.queryResponse;
  }

  public void setQueryResponse(@Nullable ChannelMessage queryResponse) {
    Preconditions.checkArgument(this.query, "Cannot set query response of no query");
    this.queryResponse = queryResponse;
  }
}
