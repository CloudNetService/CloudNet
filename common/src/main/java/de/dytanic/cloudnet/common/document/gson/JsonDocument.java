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

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.dytanic.cloudnet.common.document.IDocument;
import de.dytanic.cloudnet.common.document.IPersistable;
import de.dytanic.cloudnet.common.document.IReadable;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * The Gson implementation of IDocument class. It includes simple append and remove operations, file reading and writing
 * to create simple configuration files
 */
public class JsonDocument implements IDocument<JsonDocument> {

  @Internal
  public static final Gson GSON = new GsonBuilder()
    .serializeNulls()
    .setPrettyPrinting()
    .disableHtmlEscaping()
    .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
    .registerTypeHierarchyAdapter(Path.class, new PathTypeAdapter())
    .registerTypeHierarchyAdapter(SimpleDateFormat.class, new SimpleDateFormatTypeAdapter())
    .registerTypeHierarchyAdapter(JsonDocument.class, new JsonDocumentTypeAdapter())
    .create();
  private static final JsonDocument EMPTY = JsonDocument.newDocument();

  /* package */ final JsonObject object;

  protected JsonDocument() {
    this(new JsonObject());
  }

  protected JsonDocument(@NotNull JsonObject object) {
    this.object = object;
  }

  public static @NotNull JsonDocument empty() {
    return JsonDocument.EMPTY;
  }

  public static @NotNull JsonDocument newDocument() {
    return new JsonDocument();
  }

  public static @NotNull JsonDocument newDocument(@Nullable Object value) {
    return new JsonDocument(value == null ? new JsonObject() : GSON.toJsonTree(value).getAsJsonObject());
  }

  public static @NotNull JsonDocument newDocument(@NotNull String key, @Nullable Object value) {
    // we can ignore null
    if (value == null) {
      return JsonDocument.newDocument().append(key, (Object) null);
    }
    // append the correct type for the value
    if (value instanceof Number) {
      return JsonDocument.newDocument().append(key, (Number) value);
    } else if (value instanceof Character) {
      return JsonDocument.newDocument().append(key, (Character) value);
    } else if (value instanceof String) {
      return JsonDocument.newDocument().append(key, (String) value);
    } else if (value instanceof Boolean) {
      return JsonDocument.newDocument().append(key, (Boolean) value);
    } else if (value instanceof JsonDocument) {
      return JsonDocument.newDocument().append(key, (JsonDocument) value);
    } else {
      return JsonDocument.newDocument().append(key, value);
    }
  }

  public static @NotNull JsonDocument fromJsonBytes(byte[] bytes) {
    return fromJsonString(new String(bytes, StandardCharsets.UTF_8));
  }

  public static @NotNull JsonDocument fromJsonString(@NotNull String json) {
    return new JsonDocument(JsonParser.parseString(json).getAsJsonObject());
  }

  public static @NotNull JsonDocument newDocument(@NotNull InputStream stream) {
    var document = JsonDocument.newDocument();
    document.read(stream);
    return document;
  }

  public static @NotNull JsonDocument newDocument(@NotNull Path path) {
    var document = JsonDocument.newDocument();
    document.read(path);
    return document;
  }

  @Override
  public @NotNull Collection<String> keys() {
    return this.object.keySet();
  }

  @Override
  public int size() {
    return this.object.size();
  }

  @Override
  public @NotNull JsonDocument clear() {
    for (var key : ImmutableSet.copyOf(this.object.keySet())) {
      this.object.remove(key);
    }

    return this;
  }

  @Override
  public @NotNull JsonDocument remove(@NotNull String key) {
    this.object.remove(key);
    return this;
  }

  @Override
  public boolean contains(@NotNull String key) {
    return this.object.has(key);
  }

  @Override
  public <T> @UnknownNullability T toInstanceOf(@NotNull Class<T> clazz) {
    return GSON.fromJson(this.object, clazz);
  }

  @Override
  public <T> @UnknownNullability T toInstanceOf(@NotNull Type clazz) {
    return GSON.fromJson(this.object, clazz);
  }

  @Override
  public @NotNull JsonDocument append(@NotNull String key, @Nullable Object value) {
    this.object.add(key, value == null ? JsonNull.INSTANCE : GSON.toJsonTree(value));
    return this;
  }

  @Override
  public @NotNull JsonDocument append(@NotNull String key, @Nullable Number value) {
    this.object.addProperty(key, value);
    return this;
  }

  @Override
  public @NotNull JsonDocument append(@NotNull String key, @Nullable Boolean value) {
    this.object.addProperty(key, value);
    return this;
  }

  @Override
  public @NotNull JsonDocument append(@NotNull String key, @Nullable String value) {
    this.object.addProperty(key, value);
    return this;
  }

  @Override
  public @NotNull JsonDocument append(@NotNull String key, @Nullable Character value) {
    this.object.addProperty(key, value);
    return this;
  }

  @Override
  public @NotNull JsonDocument append(@NotNull String key, @Nullable JsonDocument value) {
    this.object.add(key, value == null ? JsonNull.INSTANCE : value.object);
    return this;
  }

  @Override
  public @NotNull JsonDocument append(@Nullable JsonDocument document) {
    if (document != null) {
      for (var entry : document.object.entrySet()) {
        this.object.add(entry.getKey(), entry.getValue());
      }
    }

    return this;
  }

  @Override
  public @NotNull JsonDocument appendNull(@NotNull String key) {
    this.object.add(key, JsonNull.INSTANCE);
    return this;
  }

  @Override
  public @NotNull JsonDocument getDocument(@NotNull String key) {
    return this.getDocument(key, JsonDocument.newDocument());
  }

  @Override
  public int getInt(@NotNull String key, int def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsInt();
  }

  @Override
  public double getDouble(@NotNull String key, double def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsDouble();
  }

  @Override
  public float getFloat(@NotNull String key, float def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsFloat();
  }

  @Override
  public byte getByte(@NotNull String key, byte def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsByte();
  }

  @Override
  public short getShort(@NotNull String key, short def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsShort();
  }

  @Override
  public long getLong(@NotNull String key, long def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsLong();
  }

  @Override
  public boolean getBoolean(@NotNull String key, boolean def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsBoolean();
  }

  @Override
  public @UnknownNullability String getString(@NotNull String key, @Nullable String def) {
    var element = this.object.get(key);
    return element == null || !element.isJsonPrimitive() ? def : element.getAsString();
  }

  @Override
  public char getChar(@NotNull String key, char def) {
    var fullString = this.getString(key);
    return fullString != null && fullString.length() > 0 ? fullString.charAt(0) : def;
  }

  @Override
  public @UnknownNullability Object get(@NotNull String key, @Nullable Object def) {
    var element = this.object.get(key);
    return element != null ? element : def;
  }

  @Override
  public <T> @UnknownNullability T get(@NotNull String key, @NotNull Class<T> clazz, @Nullable T def) {
    var element = this.object.get(key);
    return element == null || element.isJsonNull() ? def : GSON.fromJson(element, clazz);
  }

  @Override
  public <T> @UnknownNullability T get(@NotNull String key, @NotNull Type type, @Nullable T def) {
    var element = this.object.get(key);
    return element == null || element.isJsonNull() ? def : GSON.fromJson(element, type);
  }

  @Override
  public @UnknownNullability JsonDocument getDocument(@NotNull String key, @Nullable JsonDocument def) {
    var element = this.object.get(key);
    if (element != null && element.isJsonObject()) {
      return new JsonDocument(element.getAsJsonObject());
    } else {
      return def;
    }
  }

  @Override
  public @NotNull IPersistable write(@NotNull Writer writer) {
    GSON.toJson(this.object, writer);
    return this;
  }

  @Override
  public @NotNull IReadable read(@NotNull Reader reader) {
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
  public @NotNull JsonDocument properties() {
    return this;
  }

  @Override
  public @NotNull Iterator<String> iterator() {
    return this.object.keySet().iterator();
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public @NotNull JsonDocument clone() {
    return new JsonDocument(this.object.deepCopy());
  }

  public @NotNull String toPrettyJson() {
    return GSON.toJson(this.object);
  }

  @Override
  public @NotNull String toString() {
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
