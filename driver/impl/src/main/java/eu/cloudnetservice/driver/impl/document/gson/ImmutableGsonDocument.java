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

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentSerialisationException;
import eu.cloudnetservice.driver.document.SerialisationStyle;
import eu.cloudnetservice.driver.document.StandardSerialisationStyle;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.document.send.element.Element;
import eu.cloudnetservice.driver.impl.document.gson.send.GsonDocumentSend;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.utils.base.io.FileUtil;
import io.leangen.geantyref.TypeToken;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serial;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Immutable version of a gson document implementing the full document functionality.
 *
 * @since 4.0
 */
class ImmutableGsonDocument implements Document, DefaultedDocPropertyHolder {

  @Serial
  private static final long serialVersionUID = 865904160436074500L;

  protected final JsonObject internalObject;

  /**
   * Constructs a new, empty gson document instance.
   */
  public ImmutableGsonDocument() {
    this(new JsonObject());
  }

  /**
   * Constructs a new gson document instance using the given initial internal object. Note that the given object is not
   * copied, it is up to the caller to ensure no races or data leaks are created when using this constructor.
   *
   * @param internalObject the initial internal json object to use.
   * @throws NullPointerException if the given internal object is null.
   */
  ImmutableGsonDocument(@NonNull JsonObject internalObject) {
    this.internalObject = internalObject;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String factoryName() {
    return "json";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean empty() {
    return this.internalObject.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int elementCount() {
    return this.internalObject.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains(@NonNull String key) {
    return this.internalObject.has(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsNonNull(@NonNull String key) {
    var element = this.internalObject.get(key);
    return element != null && !element.isJsonNull();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DocumentSend send() {
    return GsonDocumentSend.fromJsonObject(this.internalObject);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document immutableCopy() {
    var objectCopy = this.internalObject.deepCopy();
    return new ImmutableGsonDocument(objectCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable mutableCopy() {
    var objectCopy = this.internalObject.deepCopy();
    return new MutableGsonDocument(objectCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NonNull Set<String> keys() {
    return Set.copyOf(this.internalObject.keySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NonNull Collection<? extends Element> elements() {
    // easiest way to get this is by converting this object to a Send and returning the root elements
    var documentSend = this.send();
    return documentSend.rootElement().elements();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull Type type) {
    return GsonProvider.NORMAL_GSON_INSTANCE.fromJson(this.internalObject, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull Class<T> type) {
    return GsonProvider.NORMAL_GSON_INSTANCE.fromJson(this.internalObject, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T toInstanceOf(@NonNull TypeToken<T> type) {
    return this.toInstanceOf(type.getType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T readObject(@NonNull String key, @NonNull Type type, @Nullable T def) {
    var objectElement = this.internalObject.get(key);
    return objectElement == null ? def : GsonProvider.NORMAL_GSON_INSTANCE.fromJson(objectElement, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T readObject(@NonNull String key, @NonNull Class<T> type, @Nullable T def) {
    var objectElement = this.internalObject.get(key);
    return objectElement == null ? def : GsonProvider.NORMAL_GSON_INSTANCE.fromJson(objectElement, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T readObject(@NonNull String key, @NonNull TypeToken<T> type, @Nullable T def) {
    return this.readObject(key, type.getType(), def);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnknownNullability Document readDocument(@NonNull String key, @Nullable Document def) {
    var documentElement = this.getElementSafe(key);
    if (documentElement.isJsonObject()) {
      var documentObject = documentElement.getAsJsonObject();
      return new ImmutableGsonDocument(documentObject.deepCopy());
    } else {
      return def;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Document.@UnknownNullability Mutable readMutableDocument(@NonNull String key, @Nullable Document.Mutable def) {
    var documentElement = this.getElementSafe(key);
    if (documentElement.isJsonObject()) {
      var documentObject = documentElement.getAsJsonObject();
      return new MutableGsonDocument(documentObject.deepCopy());
    } else {
      return def;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte getByte(@NonNull String key, byte def) {
    var primitiveElement = this.getPrimitiveElement(key);
    return primitiveElement != null && primitiveElement.isNumber() ? primitiveElement.getAsByte() : def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public short getShort(@NonNull String key, short def) {
    var primitiveElement = this.getPrimitiveElement(key);
    return primitiveElement != null && primitiveElement.isNumber() ? primitiveElement.getAsShort() : def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getInt(@NonNull String key, int def) {
    var primitiveElement = this.getPrimitiveElement(key);
    return primitiveElement != null && primitiveElement.isNumber() ? primitiveElement.getAsInt() : def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLong(@NonNull String key, long def) {
    var primitiveElement = this.getPrimitiveElement(key);
    return primitiveElement != null && primitiveElement.isNumber() ? primitiveElement.getAsLong() : def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float getFloat(@NonNull String key, float def) {
    var primitiveElement = this.getPrimitiveElement(key);
    return primitiveElement != null && primitiveElement.isNumber() ? primitiveElement.getAsFloat() : def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getDouble(@NonNull String key, double def) {
    var primitiveElement = this.getPrimitiveElement(key);
    return primitiveElement != null && primitiveElement.isNumber() ? primitiveElement.getAsDouble() : def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getBoolean(@NonNull String key, boolean def) {
    var primitiveElement = this.getPrimitiveElement(key);
    return primitiveElement != null && primitiveElement.isBoolean() ? primitiveElement.getAsBoolean() : def;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnknownNullability String getString(@NonNull String key, @Nullable String def) {
    var primitiveElement = this.getPrimitiveElement(key);
    return primitiveElement != null && primitiveElement.isString() ? primitiveElement.getAsString() : def;
  }

  /**
   * Internal helper method to get a json element as JsonPrimitive if it is one, else returns null. This method is
   * purely to make the code in the methods that return primitive values more readable and should not be used
   * externally.
   *
   * @param key the key of the json primitive to get.
   * @return the json primitive instance of the json element associated with the key, null if not a primitive entry.
   * @throws NullPointerException if the given key is null.
   */
  private @Nullable JsonPrimitive getPrimitiveElement(@NonNull String key) {
    var element = this.getElementSafe(key);
    return element.isJsonPrimitive() ? element.getAsJsonPrimitive() : null;
  }

  /**
   * Internal helper method to safely read a json element from the underlying json object. In comparison to the
   * JsonObject.get(String) method, this method returns the jvm-static JsonNull instance if no element with the given
   * key exists.
   *
   * @param key the key to get the json element of.
   * @return the json element associated with the key, or the jvm-static JsonNull instance if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  private @NonNull JsonElement getElementSafe(@NonNull String key) {
    var element = this.internalObject.get(key);
    return Objects.requireNonNullElse(element, JsonNull.INSTANCE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeTo(@NonNull Path path, @NonNull SerialisationStyle style) {
    FileUtil.createDirectory(path.getParent());
    try (var outputStream = Files.newOutputStream(path)) {
      this.writeTo(outputStream, style);
    } catch (IOException exception) {
      throw new DocumentSerialisationException("Unable to write document to " + path, exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeTo(@NonNull OutputStream stream, @NonNull SerialisationStyle style) {
    try (var writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
      this.writeTo(writer, style);
    } catch (IOException exception) {
      throw new DocumentSerialisationException(exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeTo(@NonNull Appendable appendable, @NonNull SerialisationStyle style) {
    try {
      var serialisationGsonInstance = this.resolveSerialisationGsonInstance(style);
      serialisationGsonInstance.toJson(this.internalObject, appendable);
    } catch (JsonIOException exception) {
      throw new DocumentSerialisationException(exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeTo(@NonNull DataBuf.Mutable dataBuf, @NonNull SerialisationStyle style) {
    var encodedJson = this.serializeToString(style);
    dataBuf.writeString(encodedJson);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String serializeToString(@NonNull SerialisationStyle style) {
    try {
      var serialisationGsonInstance = this.resolveSerialisationGsonInstance(style);
      return serialisationGsonInstance.toJson(this.internalObject);
    } catch (JsonIOException exception) {
      throw new DocumentSerialisationException(exception);
    }
  }

  /**
   * Resolves the gson instance to use to serialize this document, throwing an exception if the given serialisation
   * style is not supported.
   *
   * @param style the requested serialisation style.
   * @return the gson instance to use to serialize this document with the given style.
   * @throws NullPointerException          if the given style is null.
   * @throws UnsupportedOperationException if the given style is not supported.
   */
  private @NonNull Gson resolveSerialisationGsonInstance(@NonNull SerialisationStyle style) {
    if (style == StandardSerialisationStyle.PRETTY) {
      return GsonProvider.PRETTY_PRINTING_GSON_INSTANCE;
    }

    if (style == StandardSerialisationStyle.COMPACT) {
      return GsonProvider.NORMAL_GSON_INSTANCE;
    }

    throw new UnsupportedOperationException("Unsupported serialisation style: " + style);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document propertyHolder() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof ImmutableGsonDocument document) {
      return Objects.equals(this.internalObject, document.internalObject);
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.internalObject);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return this.serializeToString(StandardSerialisationStyle.COMPACT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeExternal(@NonNull ObjectOutput out) throws IOException {
    out.writeUTF(this.serializeToString(StandardSerialisationStyle.COMPACT));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void readExternal(@NonNull ObjectInput in) throws IOException {
    var parsedDocument = JsonParser.parseString(in.readUTF());
    Preconditions.checkArgument(parsedDocument.isJsonObject(), "Input is not a json object");

    // put all elements of the parsed object into the internal object - we don't need to
    // clone the elements as they are freshly parsed and never shared anywhere.
    var object = parsedDocument.getAsJsonObject();
    for (var entry : object.entrySet()) {
      this.internalObject.add(entry.getKey(), entry.getValue());
    }
  }
}
