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

package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import org.junit.Assert;
import org.junit.Test;

public class ChannelMessageSerializerTest {

  @Test
  public void serializeChannelMessage() {
    ChannelMessage original = ChannelMessage.builder(new ChannelMessageSender("Test", DriverEnvironment.CLOUDNET))
      .channel("test-channel")
      .message("test-message")
      .buffer(new byte[]{1, 2, 3, 4, 5})
      .json(JsonDocument.newDocument("1", "2"))
      .targetAll(ChannelMessageTarget.Type.NODE)
      .build();

    ProtocolBuffer buffer = ProtocolBuffer.create();
    buffer.writeObject(original);

    ChannelMessage deserialized = buffer.readObject(ChannelMessage.class);

    Assert.assertEquals(original, deserialized);
  }

}
