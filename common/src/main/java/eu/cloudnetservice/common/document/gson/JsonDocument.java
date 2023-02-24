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

package eu.cloudnetservice.common.document.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import eu.cloudnetservice.common.document.Document;
import eu.cloudnetservice.common.document.Persistable;
import eu.cloudnetservice.common.document.Readable;
import eu.cloudnetservice.common.document.property.DefaultedDocPropertyHolder;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * The Gson implementation of IDocument class. It includes simple append and remove operations, file reading and writing
 * to create simple configuration files
 */
public final class JsonDocument
  implements DefaultedDocPropertyHolder<JsonDocument, JsonDocument>, Document<JsonDocument> {

  @ApiStatus.Internal
  public static final Gson GSON = new GsonBuilder()
    .serializeNulls()
    .setPrettyPrinting()
    .disableHtmlEscaping()
    .registerTypeHierarchyAdapter(Path.class, new PathTypeAdapter())
    .registerTypeHierarchyAdapter(Pattern.class, new PatternTypeAdapter())
    .registerTypeHierarchyAdapter(JsonDocument.class, new JsonDocumentTypeAdapter())
    .create();
  private static final JsonDocument EMPTY = JsonDocument.newDocument();

  /* package */ final JsonObject object;

  JsonDocument() {
    this(new JsonObject());
  }

  JsonDocument(@NonNull JsonObject object) {
    this.object = object;
  }

  public static @NonNull JsonDocument emptyDocument() {
    return JsonDocument.EMPTY;
  }

  public static @NonNull JsonDocument newDocument() {
    return new JsonDocument();
  }

  public static @NonNull JsonDocument newDocument(@Nullable Object value) {
    return new JsonDocument(value == null ? new JsonObject() : GSON.toJsonTree(value).getAsJsonObject());
  }

  public static @NonNull JsonDocument newDocument(@NonNull String key, @Nullable Object value) {
    // we can ignore null
    if (value == null) {
      return JsonDocument.newDocument().append(key, (Object) null);
    }
    // append the correct type for the value
    if (value instanceof Number number) {
      return JsonDocument.newDocument().append(key, number);
    } else if (value instanceof Character character) {
      return JsonDocument.newDocument().append(key, character);
    } else if (value instanceof String string) {
      return JsonDocument.newDocument().append(key, string);
    } else if (value instanceof Boolean bool) {
      return JsonDocument.newDocument().append(key, bool);
    } else if (value instanceof JsonDocument document) {
      return JsonDocument.newDocument().append(key, document);
    } else {
      return JsonDocument.newDocument().append(key, value);
    }
  }

  public static @NonNull JsonDocument fromJsonBytes(byte[] bytes) {
    return fromJsonString(new String(bytes, StandardCharsets.UTF_8));
  }

  public static @NonNull JsonDocument fromJsonString(@NonNull String json) {
    return new JsonDocument(JsonParser.parseString(json).getAsJsonObject());
  }

  public static @NonNull JsonDocument newDocument(@NonNull InputStream stream) {
    var document = JsonDocument.newDocument();
    document.read(stream);
    return document;
  }

  public static @NonNull JsonDocument newDocument(@NonNull Path path) {
    var document = JsonDocument.newDocument();
    document.read(path);
    return document;
  }

  @Override
  public @NonNull Collection<String> keys() {
    return this.object.keySet();
  }

  @Override
  public int size() {
    return this.object.size();
  }

  @Override
  public @NonNull JsonDocument clear() {
    for (var key : Set.copyOf(this.object.keySet())) {
      this.object.remove(key);
    }

    return this;
  }

  @Override
  public @NonNull JsonDocument remove(@NonNull String key) {
    this.object.remove(key);
    return this;
  }

  @Override
  public boolean contains(@NonNull String key) {
    return this.object.has(key);
  }

  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull Class<T> clazz) {
    return GSON.fromJson(this.object, clazz);
  }

  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull Type clazz) {
    return GSON.fromJson(this.object, clazz);
  }

  @Override
  public @NonNull JsonDocument append(@NonNull String key, @Nullable Object value) {
    this.object.add(key, value == null ? JsonNull.INSTANCE : GSON.toJsonTree(value));
    return this;
  }

  @Override
  public @NonNull JsonDocument append(@NonNull String key, @Nullable Number value) {
    this.object.addProperty(key, value);
    return this;
  }

  @Override
  public @NonNull JsonDocument append(@NonNull String key, @Nullable Boolean value) {
    this.object.addProperty(key, value);
    return this;
  }

  @Override
  public @NonNull JsonDocument append(@NonNull String key, @Nullable String value) {
    this.object.addProperty(key, value);
    return this;
  }

  @Override
  public @NonNull JsonDocument append(@NonNull String key, @Nullable Character value) {
    this.object.addProperty(key, value);
    return this;
  }

  @Override
  public @NonNull JsonDocument append(@NonNull String key, @Nullable JsonDocument value) {
    this.object.add(key, value == null ? JsonNull.INSTANCE : value.object);
    return this;
  }

  @Override
  public @NonNull JsonDocument append(@Nullable JsonDocument document) {
    if (document != null) {
      for (var entry : document.object.entrySet()) {
        this.object.add(entry.getKey(), entry.getValue());
      }
    }

    return this;
  }

  @Override
  public @NonNull JsonDocument appendNull(@NonNull String key) {
    this.object.add(key, JsonNull.INSTANCE);
    return this;
  }

  @Override
  public @NonNull JsonDocument getDocument(@NonNull String key) {
    return this.getDocument(key, JsonDocument.newDocument());
  }

  @Override
  public int getInt(@NonNull String key, int def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsInt();
  }

  @Override
  public double getDouble(@NonNull String key, double def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsDouble();
  }

  @Override
  public float getFloat(@NonNull String key, float def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsFloat();
  }

  @Override
  public byte getByte(@NonNull String key, byte def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsByte();
  }

  @Override
  public short getShort(@NonNull String key, short def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsShort();
  }

  @Override
  public long getLong(@NonNull String key, long def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsLong();
  }

  @Override
  public boolean getBoolean(@NonNull String key, boolean def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsBoolean();
  }

  @Override
  public @UnknownNullability String getString(@NonNull String key, @Nullable String def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsString();
  }

  @Override
  public char getChar(@NonNull String key, char def) {
    var fullString = this.getString(key);
    return fullString != null && fullString.length() > 0 ? fullString.charAt(0) : def;
  }

  @Override
  public @UnknownNullability Object get(@NonNull String key, @Nullable Object def) {
    var element = this.object.get(key);
    return element == null || element.isJsonNull() ? def : element;
  }

  @Override
  public <T> @UnknownNullability T get(@NonNull String key, @NonNull Class<T> clazz, @Nullable T def) {
    var element = this.object.get(key);
    return element == null || element.isJsonNull() ? def : GSON.fromJson(element, clazz);
  }

  @Override
  public <T> @UnknownNullability T get(@NonNull String key, @NonNull Type type, @Nullable T def) {
    var element = this.object.get(key);
    return element == null || element.isJsonNull() ? def : GSON.fromJson(element, type);
  }

  @Override
  public @UnknownNullability JsonDocument getDocument(@NonNull String key, @Nullable JsonDocument def) {
    var element = this.object.get(key);
    if (element != null && element.isJsonObject()) {
      return new JsonDocument(element.getAsJsonObject());
    } else {
      return def;
    }
  }

  @Override
  public @NonNull Persistable write(@NonNull Writer writer) {
    GSON.toJson(this.object, writer);
    return this;
  }

  @Override
  public @NonNull Readable read(@NonNull Reader reader) {
    try {
      // parse the object
      var element = JsonParser.parseReader(reader);
      if (element.isJsonObject()) {
        for (var entry : element.getAsJsonObject().entrySet()) {
          this.object.add(entry.getKey(), entry.getValue());
        }
        return this;
      }
      // not a json object - unable to parse
      throw new JsonSyntaxException("Json element parsed from reader is not a json object");
    } catch (Exception exception) {
      throw new RuntimeException("Unable to parse json document from reader", exception);
    }
  }

  @Override
  public @NonNull JsonDocument propertyHolder() {
    return this;
  }

  @Override
  public @NonNull Iterator<String> iterator() {
    return this.object.keySet().iterator();
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public @NonNull JsonDocument clone() {
    return new JsonDocument(this.object.deepCopy());
  }

  public @NonNull String toPrettyJson() {
    return GSON.toJson(this.object);
  }

  @Override
  public @NonNull String toString() {
    return this.object.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JsonDocument document)) {
      return false;
    }

    return document.object.equals(this.object);
  }

  @Override
  public int hashCode() {
    return this.object.hashCode();
  }
}
