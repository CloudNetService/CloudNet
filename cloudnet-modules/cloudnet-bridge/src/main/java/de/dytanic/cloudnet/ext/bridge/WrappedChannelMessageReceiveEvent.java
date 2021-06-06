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

package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WrappedChannelMessageReceiveEvent {

  ChannelMessageReceiveEvent getWrapped();

  @NotNull
  default ChannelMessageSender getSender() {
    return this.getWrapped().getSender();
  }

  @NotNull
  default Collection<ChannelMessageTarget> getTargets() {
    return this.getWrapped().getTargets();
  }

  @NotNull
  default String getChannel() {
    return this.getWrapped().getChannel();
  }

  @Nullable
  default String getMessage() {
    return this.getWrapped().getMessage();
  }

  @NotNull
  default ChannelMessage getChannelMessage() {
    return this.getWrapped().getChannelMessage();
  }

  @NotNull
  default JsonDocument getData() {
    return this.getWrapped().getData();
  }

  @NotNull
  default ProtocolBuffer getBuffer() {
    return this.getWrapped().getBuffer();
  }

  default boolean isQuery() {
    return this.getWrapped().isQuery();
  }

  default void setQueryResponse(@Nullable ChannelMessage queryResponse) {
    this.getWrapped().setQueryResponse(queryResponse);
  }

  default void setJsonResponse(@NotNull JsonDocument json) {
    this.getWrapped().setJsonResponse(json);
  }

  default void setBinaryResponse(@NotNull ProtocolBuffer buffer) {
    this.getWrapped().setBinaryResponse(buffer);
  }

  default ProtocolBuffer createBinaryResponse() {
    return this.getWrapped().createBinaryResponse();
  }

}
