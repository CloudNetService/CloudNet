/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.document.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.cloudnetservice.driver.document.Document;
import java.nio.file.Path;
import java.util.regex.Pattern;

final class GsonProvider {

  static final Gson NORMAL_GSON_INSTANCE = new GsonBuilder()
    .serializeNulls()
    .disableHtmlEscaping()
    .registerTypeAdapter(Pattern.class, new PatternTypeAdapter())
    .registerTypeHierarchyAdapter(Path.class, new PathTypeAdapter())
    .registerTypeHierarchyAdapter(Document.class, new DocumentTypeAdapter())
    .setFieldNamingStrategy(GsonDocumentFieldNamingStrategy.INSTANCE)
    .addSerializationExclusionStrategy(GsonDocumentExclusionStrategy.SERIALIZE)
    .addDeserializationExclusionStrategy(GsonDocumentExclusionStrategy.DESERIALIZE)
    .create();
  static final Gson PRETTY_PRINTING_GSON_INSTANCE = NORMAL_GSON_INSTANCE.newBuilder()
    .setPrettyPrinting()
    .create();

  private GsonProvider() {
    throw new UnsupportedOperationException();
  }
}
