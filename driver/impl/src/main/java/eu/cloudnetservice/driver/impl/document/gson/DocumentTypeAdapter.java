/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.impl.document.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import eu.cloudnetservice.driver.document.Document;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A type adapter that can serialize documents of any type into a json object and read documents into a json type.
 *
 * @since 4.0
 */
final class DocumentTypeAdapter extends TypeAdapter<Document> {

  private final TypeAdapter<JsonElement> jsonElementTypeAdapter;

  /**
   * Constructs a new instance of the document type adapter.
   *
   * @param gsonInstance the gson instance this type adapter instance got bound to.
   * @throws NullPointerException if the given gson instance is null.
   */
  public DocumentTypeAdapter(@NonNull Gson gsonInstance) {
    this.jsonElementTypeAdapter = gsonInstance.getAdapter(JsonElement.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(@NonNull JsonWriter out, @Nullable Document value) throws IOException {
    if (value == null) {
      // document not given, just write an empty document
      out.beginObject().endObject();
      return;
    }

    // check if the document is already in json form
    if (value instanceof ImmutableGsonDocument gsonDocument) {
      this.jsonElementTypeAdapter.write(out, gsonDocument.internalObject);
      return;
    }

    // convert the document to json
    var targetDocument = new MutableGsonDocument();
    targetDocument.receive(value.send());
    this.jsonElementTypeAdapter.write(out, targetDocument.internalObject);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document read(@NonNull JsonReader in) throws IOException {
    // read the json element and validate that is a json object
    var jsonElement = this.jsonElementTypeAdapter.read(in);
    if (jsonElement != null && jsonElement.isJsonObject()) {
      return new ImmutableGsonDocument(jsonElement.getAsJsonObject());
    }

    // return a new, empty document if the deserialized element is not a json object
    return new ImmutableGsonDocument();
  }
}
