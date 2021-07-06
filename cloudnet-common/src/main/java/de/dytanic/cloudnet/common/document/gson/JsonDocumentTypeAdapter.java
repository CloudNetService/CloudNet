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

package de.dytanic.cloudnet.common.document.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

public class JsonDocumentTypeAdapter extends TypeAdapter<JsonDocument> {

  @Override
  @Deprecated // not part of the api
  public void write(JsonWriter jsonWriter, JsonDocument document) throws IOException {
    TypeAdapters.JSON_ELEMENT.write(jsonWriter, document == null ? new JsonObject() : document.jsonObject);
  }

  @Override
  @Deprecated // not part of the api
  public JsonDocument read(JsonReader jsonReader) throws IOException {
    JsonElement jsonElement = TypeAdapters.JSON_ELEMENT.read(jsonReader);
    if (jsonElement != null && jsonElement.isJsonObject()) {
      return new JsonDocument(jsonElement);
    } else {
      return null;
    }
  }

}
