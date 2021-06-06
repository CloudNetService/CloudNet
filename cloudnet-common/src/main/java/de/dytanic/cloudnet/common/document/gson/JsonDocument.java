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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.bind.TypeAdapters;
import de.dytanic.cloudnet.common.document.IDocument;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * The Gson implementation of IDocument class. It includes simple append and remove operations, file reading and writing
 * to create simple configuration files
 */
@EqualsAndHashCode
public class JsonDocument implements IDocument<JsonDocument>, Cloneable {

  public static final JsonDocument EMPTY = newDocument();
  public static final Gson GSON = new GsonBuilder()
    .serializeNulls()
    .disableHtmlEscaping()
    .setPrettyPrinting()
    .registerTypeAdapterFactory(TypeAdapters.newTypeHierarchyFactory(JsonDocument.class, new JsonDocumentTypeAdapter()))
    .create();
  protected final JsonObject jsonObject;

  /**
   * @deprecated causes issues because of the relocated Gson, will be made private in a future release
   */
  @Deprecated
  public JsonDocument(JsonObject jsonObject) {
    this.jsonObject = jsonObject;
  }

  public JsonDocument() {
    this(new JsonObject());
  }

  public JsonDocument(Object toObjectMirror) {
    this(GSON.toJsonTree(toObjectMirror));
  }

  /**
   * @deprecated causes issues because of the relocated Gson, will be made private in a future release
   */
  @Deprecated
  public JsonDocument(JsonElement jsonElement) {
    this(jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : new JsonObject());
  }

  public JsonDocument(Properties properties) {
    this();
    this.append(properties);
  }

  public JsonDocument(String key, String value) {
    this();
    this.append(key, value);
  }

  public JsonDocument(String key, Object value) {
    this();
    this.append(key, value);
  }

  public JsonDocument(String key, Boolean value) {
    this();
    this.append(key, value);
  }

  public JsonDocument(String key, Number value) {
    this();
    this.append(key, value);
  }

  public JsonDocument(String key, Character value) {
    this();
    this.append(key, value);
  }

  public JsonDocument(String key, JsonDocument value) {
    this();
    this.append(key, value);
  }

  public JsonDocument(String key, Properties value) {
    this();
    this.append(key, value);
  }


  public static JsonDocument newDocument() {
    return new JsonDocument();
  }

  /**
   * @deprecated causes issues because of the relocated Gson, will be removed in a future release
   */
  @Deprecated
  public static JsonDocument newDocument(JsonObject jsonObject) {
    return new JsonDocument(jsonObject);
  }

  public static JsonDocument newDocument(String key, String value) {
    return new JsonDocument(key, value);
  }

  public static JsonDocument newDocument(String key, Number value) {
    return new JsonDocument(key, value);
  }

  public static JsonDocument newDocument(String key, Character value) {
    return new JsonDocument(key, value);
  }

  public static JsonDocument newDocument(String key, Boolean value) {
    return new JsonDocument(key, value);
  }

  public static JsonDocument newDocument(String key, Object value) {
    return new JsonDocument(key, value);
  }


  public static JsonDocument newDocument(byte[] bytes) {
    return newDocument(new String(bytes, StandardCharsets.UTF_8));
  }

  public static JsonDocument newDocument(Object object) {
    return new JsonDocument(GSON.toJsonTree(object));
  }

  @Deprecated
  public static JsonDocument newDocument(File file) {
    if (file == null) {
      return null;
    }

    return newDocument(file.toPath());
  }

  public static JsonDocument newDocument(Path path) {
    JsonDocument document = new JsonDocument();

    document.read(path);
    return document;
  }

  public static JsonDocument newDocument(InputStream stream) {
    JsonDocument document = new JsonDocument();
    document.read(stream);
    return document;
  }

  public static JsonDocument newDocument(String json) {
    try {
      return new JsonDocument(JsonParser.parseString(json));
    } catch (JsonSyntaxException exception) {
      exception.printStackTrace();
      return new JsonDocument();
    }
  }

  @Override
  public Collection<String> keys() {
    Collection<String> collection = new ArrayList<>(this.jsonObject.size());

    for (Map.Entry<String, JsonElement> entry : this.jsonObject.entrySet()) {
      collection.add(entry.getKey());
    }

    return collection;
  }

  @Override
  public int size() {
    return this.jsonObject.size();
  }

  @Override
  public JsonDocument clear() {
    for (Map.Entry<String, JsonElement> elementEntry : this.jsonObject.entrySet()) {
      this.jsonObject.remove(elementEntry.getKey());
    }

    return this;
  }

  @Override
  public JsonDocument remove(String key) {
    this.jsonObject.remove(key);
    return this;
  }

  @Override
  public boolean contains(String key) {
    return key != null && this.jsonObject.has(key);
  }

  @Override
  public <T> T toInstanceOf(Class<T> clazz) {
    return GSON.fromJson(this.jsonObject, clazz);
  }

  @Override
  public <T> T toInstanceOf(Type type) {
    return GSON.fromJson(this.jsonObject, type);
  }

  @Override
  public JsonDocument append(String key, Object value) {
    if (key == null || value == null) {
      return this;
    }

    this.jsonObject.add(key, GSON.toJsonTree(value));
    return this;
  }

  @Override
  public JsonDocument append(String key, Number value) {
    if (key == null || value == null) {
      return this;
    }

    this.jsonObject.addProperty(key, value);
    return this;
  }

  @Override
  public JsonDocument append(String key, Boolean value) {
    if (key == null || value == null) {
      return this;
    }

    this.jsonObject.addProperty(key, value);
    return this;
  }

  @Override
  public JsonDocument append(String key, String value) {
    if (key == null || value == null) {
      return this;
    }

    this.jsonObject.addProperty(key, value);
    return this;
  }

  @Override
  public JsonDocument append(String key, Character value) {
    if (key == null || value == null) {
      return this;
    }

    this.jsonObject.addProperty(key, value);
    return this;
  }

  @Override
  public JsonDocument append(String key, JsonDocument value) {
    if (key == null || value == null) {
      return this;
    }

    this.jsonObject.add(key, value.jsonObject);
    return this;
  }

  @Override
  public JsonDocument append(JsonDocument document) {
    if (document == null) {
      return this;
    } else {
      return this.append(document.jsonObject);
    }
  }

  /**
   * @deprecated causes issues because of the relocated Gson, will be made private in a future release
   */
  @Deprecated
  public JsonDocument append(JsonObject jsonObject) {
    if (jsonObject == null) {
      return this;
    }

    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
      this.jsonObject.add(entry.getKey(), entry.getValue());
    }

    return this;
  }

  @Override
  public JsonDocument append(Properties properties) {
    if (properties == null) {
      return this;
    }

    Object entry;
    Enumeration<?> enumeration = properties.keys();

    while (enumeration.hasMoreElements() && (entry = enumeration.nextElement()) != null) {
      this.append(entry.toString(), properties.getProperty(entry.toString()));
    }

    return this;
  }

  @Override
  public JsonDocument append(String key, Properties properties) {
    return this.append(key, new JsonDocument(properties));
  }

  @Override
  public JsonDocument append(String key, byte[] bytes) {
    if (key == null || bytes == null) {
      return this;
    }

    return this.append(key, Base64.getEncoder().encodeToString(bytes));
  }

  @Override
  public JsonDocument append(Map<String, Object> map) {
    if (map == null) {
      return this;
    }

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      this.append(entry.getKey(), entry.getValue());
    }

    return this;
  }

  @Override
  public @NotNull JsonDocument append(@NotNull Reader reader) {
    return this.append(JsonParser.parseReader(reader).getAsJsonObject());
  }

  @Override
  public JsonDocument appendNull(String key) {
    if (key == null) {
      return this;
    }

    this.jsonObject.add(key, JsonNull.INSTANCE);
    return this;
  }

  @Override
  public JsonDocument getDocument(String key) {
    if (!this.contains(key)) {
      return null;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonObject()) {
      return new JsonDocument(jsonElement);
    } else {
      return null;
    }
  }

  @Override
  public int getInt(String key) {
    if (!this.contains(key)) {
      return 0;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsInt();
    } else {
      return 0;
    }
  }

  @Override
  public double getDouble(String key) {
    if (!this.contains(key)) {
      return 0;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsDouble();
    } else {
      return 0;
    }
  }

  @Override
  public float getFloat(String key) {
    if (!this.contains(key)) {
      return 0;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsFloat();
    } else {
      return 0;
    }
  }

  @Override
  public byte getByte(String key) {
    if (!this.contains(key)) {
      return 0;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsByte();
    } else {
      return 0;
    }
  }

  @Override
  public short getShort(String key) {
    if (!this.contains(key)) {
      return 0;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsShort();
    } else {
      return 0;
    }
  }

  @Override
  public long getLong(String key) {
    if (!this.contains(key)) {
      return 0;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsLong();
    } else {
      return 0;
    }
  }

  @Override
  public boolean getBoolean(String key) {
    if (!this.contains(key)) {
      return false;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsBoolean();
    } else {
      return false;
    }
  }

  @Override
  public String getString(String key) {
    if (!this.contains(key)) {
      return null;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsString();
    } else {
      return null;
    }
  }

  @Override
  public char getChar(String key) {
    if (!this.contains(key)) {
      return 0;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsString().charAt(0);
    } else {
      return 0;
    }
  }

  @Override
  public BigDecimal getBigDecimal(String key) {
    if (!this.contains(key)) {
      return null;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsBigDecimal();
    } else {
      return null;
    }
  }

  @Override
  public BigInteger getBigInteger(String key) {
    if (!this.contains(key)) {
      return null;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonPrimitive()) {
      return jsonElement.getAsBigInteger();
    } else {
      return null;
    }
  }

  /**
   * @deprecated causes issues because of the relocated Gson, will be removed in a future release
   */
  @Deprecated
  public JsonArray getJsonArray(String key) {
    if (!this.contains(key)) {
      return null;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonArray()) {
      return jsonElement.getAsJsonArray();
    } else {
      return null;
    }
  }

  /**
   * @deprecated causes issues because of the relocated Gson, will be removed in a future release
   */
  @Deprecated
  public JsonObject getJsonObject(String key) {
    if (!this.contains(key)) {
      return null;
    }

    JsonElement jsonElement = this.jsonObject.get(key);

    if (jsonElement.isJsonObject()) {
      return jsonElement.getAsJsonObject();
    } else {
      return null;
    }
  }

  @Override
  public Properties getProperties(String key) {
    Properties properties = new Properties();

    for (Map.Entry<String, JsonElement> entry : this.jsonObject.entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue().toString());
    }

    return properties;
  }

  /**
   * @deprecated causes issues because of the relocated Gson, will be removed in a future release
   */
  @Deprecated
  public JsonElement get(String key) {
    if (!this.contains(key)) {
      return null;
    }

    return this.jsonObject.get(key);
  }

  @Override
  public byte[] getBinary(String key) {
    return Base64.getDecoder().decode(this.getString(key));
  }

  @Override
  public <T> T get(String key, Class<T> clazz) {
    return this.get(key, GSON, clazz);
  }

  @Override
  public <T> T get(String key, Type type) {
    return this.get(key, GSON, type);
  }

  public <T> T get(String key, Gson gson, Class<T> clazz) {
    if (key == null || gson == null || clazz == null) {
      return null;
    }

    JsonElement jsonElement = this.get(key);

    if (jsonElement == null) {
      return null;
    } else {
      return gson.fromJson(jsonElement, clazz);
    }
  }

  public <T> T get(String key, Gson gson, Type type) {
    if (key == null || gson == null || type == null) {
      return null;
    }

    if (!this.contains(key)) {
      return null;
    }

    JsonElement jsonElement = this.get(key);

    if (jsonElement == null) {
      return null;
    } else {
      return gson.fromJson(jsonElement, type);
    }
  }

  public Integer getInt(String key, Integer def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getInt(key);
  }

  public Short getShort(String key, Short def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getShort(key);
  }

  public Boolean getBoolean(String key, Boolean def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getBoolean(key);
  }

  public Long getLong(String key, Long def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getLong(key);
  }

  public Double getDouble(String key, Double def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getDouble(key);
  }


  public Float getFloat(String key, Float def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getFloat(key);
  }

  public String getString(String key, String def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getString(key);
  }

  public JsonDocument getDocument(String key, JsonDocument def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getDocument(key);
  }

  /**
   * @deprecated causes issues because of the relocated Gson, will be removed in a future release
   */
  @Deprecated
  public JsonArray getJsonArray(String key, JsonArray def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getJsonArray(key);
  }

  /**
   * @deprecated causes issues because of the relocated Gson, will be removed in a future release
   */
  @Deprecated
  public JsonObject getJsonObject(String key, JsonObject def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getJsonObject(key);
  }

  public byte[] getBinary(String key, byte[] def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getBinary(key);
  }


  public <T> T get(String key, Type type, T def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.get(key, type);
  }

  public <T> T get(String key, Class<T> clazz, T def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.get(key, clazz);
  }

  public Properties getProperties(String key, Properties def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getProperties(key);
  }

  public BigInteger getBigInteger(String key, BigInteger def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getBigInteger(key);
  }


  public BigDecimal getBigDecimal(String key, BigDecimal def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getBigDecimal(key);
  }

  public Character getChar(String key, Character def) {
    if (!this.contains(key)) {
      this.append(key, def);
    }

    return this.getChar(key);
  }

  @Override
  public @NotNull JsonDocument write(Writer writer) {
    GSON.toJson(this.jsonObject, writer);
    return this;
  }

  @Override
  public @NotNull JsonDocument read(@NotNull Reader reader) {
    try (BufferedReader bufferedReader = new BufferedReader(reader)) {
      return this.append(JsonParser.parseReader(bufferedReader).getAsJsonObject());
    } catch (Exception ex) {
      ex.getStackTrace();
    }
    return this;
  }

  @Override
  public @NotNull JsonDocument read(byte[] bytes) {
    this.append(JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject());
    return this;
  }

  @Override
  public @NotNull JsonDocument read(@NotNull InputStream inputStream) {
    try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      return this.read(reader);
    } catch (IOException exception) {
      exception.printStackTrace();
      return this;
    }
  }

  @Override
  public <E> JsonDocument setProperty(JsonDocProperty<E> docProperty, E val) {
    docProperty.appender.accept(val, this);
    return this;
  }

  @Override
  public <E> E getProperty(JsonDocProperty<E> docProperty) {
    return docProperty.resolver.apply(this);
  }

  @Override
  public <E> JsonDocument removeProperty(JsonDocProperty<E> docProperty) {
    docProperty.remover.accept(this);
    return this;
  }

  @Override
  public <E> boolean hasProperty(JsonDocProperty<E> docProperty) {
    return docProperty.tester.test(this);
  }

  @Override
  public JsonDocument getProperties() {
    return this;
  }

  /**
   * @deprecated causes issues because of the relocated Gson, will be removed in a future release
   */
  @Deprecated
  public JsonObject toJsonObject() {
    return this.jsonObject;
  }

  public String toPrettyJson() {
    return GSON.toJson(this.jsonObject);
  }

  public String toJson() {
    return this.jsonObject.toString();
  }

  public byte[] toByteArray() {
    return this.toJson().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String toString() {
    return this.toJson();
  }

  @NotNull
  @Override
  public Iterator<String> iterator() {
    return this.jsonObject.keySet().iterator();
  }

  @Override
  public JsonDocument clone() {
    return new JsonDocument(this.jsonObject.deepCopy());
  }
}
