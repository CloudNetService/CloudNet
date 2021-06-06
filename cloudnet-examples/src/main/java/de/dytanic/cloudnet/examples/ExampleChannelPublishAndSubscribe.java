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

package de.dytanic.cloudnet.examples;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public final class ExampleChannelPublishAndSubscribe {

  public void publishGlobalMessage() {
    ChannelMessage.builder()
      .channel("user_channel")
      .message("user_info_publishing")
      .json(JsonDocument.newDocument().append("name", "Peter Parker").append("age", 17))
      .targetAll()
      .build()
      .send();
    //Send a channel message to all services and nodes in network
  }

  public void publishMessage(String targetServiceName) {
    ChannelMessage.builder()
      .channel("user_channel")
      .message("user_info_publishing")
      .json(JsonDocument.newDocument().append("name", "Peter Parker").append("age", 17))
      .targetService(targetServiceName)
      .build()
      .send();
    //Send a channel message to a specified service
  }

  public String sendQueryToService(String targetServiceName) {
    //Send a channel message to a specified service and get a response

    ChannelMessage response = ChannelMessage.builder()
      .channel("user_channel")
      .message("user_info")
      .json(JsonDocument.newDocument().append("any request", "..."))
      .targetService(targetServiceName)
      .build()
      .sendSingleQuery();

    if (response == null) {
      return null;
    }

    String binaryResponse = response.getBuffer().readString();
    String jsonResponse = response.getJson().getString("test");
    JsonDocument requested = response.getJson().getDocument("requested");

    return jsonResponse;
  }

  @EventListener
  public void handleChannelMessage(ChannelMessageReceiveEvent event) {
    if (event.getMessage() == null) {
      return;
    }

    if (event.getChannel().equalsIgnoreCase("user_channel")) {
      if ("user_info_publishing".equals(event.getMessage().toLowerCase())) {
        System.out.println("Name: " + event.getData().getString("name") + " | Age: " + event.getData().getInt("age"));
      }
    }
    //Receive a channel message in the network
  }

  @EventListener
  public void handleQueryChannelMessage(ChannelMessageReceiveEvent event) {
    if (event.getMessage() == null || !event.isQuery()) {
      return;
    }

    if (event.getChannel().equalsIgnoreCase("user_channel")) {
      if ("user_info".equals(event.getMessage().toLowerCase())) {
        event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
          .json(JsonDocument.newDocument("test", "response string").append("requested", event.getData()))
          .buffer(ProtocolBuffer.create().writeString("binary response"))
          .build());
      }
    }
    //Receive a channel message in the network
  }

}
