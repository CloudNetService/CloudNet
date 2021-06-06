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

package eu.cloudnetservice.cloudnet.ext.labymod;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public class LabyModChannelUtils {

  private LabyModChannelUtils() {
    throw new UnsupportedOperationException();
  }

  public static byte[] getLMCMessageContents(String messageKey, JsonDocument messageContents) {
    return ProtocolBuffer.create()
      .writeString(messageKey)
      .writeString(messageContents.toJson())
      .toArray();
  }

  public static Pair<String, JsonDocument> readLMCMessageContents(byte[] data) {
    ProtocolBuffer buffer = ProtocolBuffer.wrap(data);

    String messageKey = buffer.readString();
    String messageContents = buffer.readString();
    JsonDocument document = JsonDocument.newDocument(messageContents);

    return new Pair<>(messageKey, document);
  }

}
