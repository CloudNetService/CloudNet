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

package eu.cloudnetservice.driver.document;

import eu.cloudnetservice.driver.document.property.DocPropertyHolder;
import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.document.send.element.Element;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import io.leangen.geantyref.TypeToken;
import java.io.Externalizable;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The root of a document tree. A document tree consists of key-value pairs, where the keys are string and values are
 * one of the following types:
 * <ol>
 *   <li>Objects
 *   <li>Arrays
 *   <li>Primitives
 *   <li>Null
 * </ol>
 * <p>
 * A document can contain multiple sub-documents which are represented as object types. Each of these nesting levels can
 * generically be accessed by reading the document that is associated with the key rather than parsing it to a specific
 * java representation.
 * <p>
 * Due to the fact that a document is not representing a specific implementation (for example JSON) there is no
 * guarantee that an implementation supports all methods to read elements from the underlying configuration type. In
 * these cases the called methods will throw an {@link UnsupportedOperationException}. If a conversion between different
 * document types is required, use the send api as follows:
 * <pre>
 * {@code
 *   Document jsonDocument = Document.newJsonDocument().append("json_key", "Hello World!");
 *   Document.Mutable tomlDocument = TomlDocumentSource.newDocument().append("toml_key", "CloudNet");
 *
 *   // Serializes the content of the json document into a format that
 *   // must be supported by all implementations. This action does not
 *   // affect the source document at all (all key value pairs of the
 *   // source document are still available after the method call)
 *   DocumentSend send = jsonDocument.send();
 *
 *   // Puts in the serialized content of the source document into the
 *   // target document. If the target implementation does not support
 *   // all element types they might get ignored and are not available
 *   // in the target document. This method call will override all
 *   // existing value mappings if a duplicate key is encountered.
 *   tomlDocument.receive(send);
 *   Assertions.assertEquals("CloudNet", tomlDocument.getString("toml_key")); // old key is present
 *   Assertions.assertEquals("Hello World!", tomlDocument.getString("json_key")); // key of json document is present
 *
 *   // This way a new document is created to receive the content of the
 *   // source document rather than appending to an existing one
 *   Document newTomlDocument = TomlDocumentFactory.INSTANCE.receive(send);
 *   Assertions.assertFalse(newTomlDocument.contains("toml_key")); // this key was not send from the json document
 *   Assertions.assertEquals("Hello World!", newTomlDocument.getString("json_key")); // key of json document is present
 * }
 * </pre>
 * <p>
 * There are two versions of document implementation: a mutable ({@link Mutable}) and an immutable version. The
 * immutable version has the single constraint that there are no changes made to the document after it became immutable,
 * which guarantees that reading from the same immutable document in multiple places will never result in a different
 * outcome. There is no guarantee for the mutable version of the document to be thread-safe.
 *
 * @since 4.0
 */
public interface Document extends DocPropertyHolder, Externalizable {

  /**
   * Get the jvm static implementation of an empty document. The returned document does not take any writes into account
   * and does always return the supplied default value when reading from it. Some methods might throw an exception when
   * used.
   *
   * @return the jvm static empty document instance.
   */
  static @NonNull Document.Mutable emptyDocument() {
    var emptyDocumentFactory = ServiceRegistry.registry().instance(DocumentFactory.class, "empty");
    return newDocument(emptyDocumentFactory);
  }

  /**
   * Constructs a new json document instance. The returned document is mutable.
   *
   * @return a json new document instance.
   */
  static @NonNull Document.Mutable newJsonDocument() {
    return newDocument(DocumentFactory.json());
  }

  /**
   * Constructs a new document instance using the given document factory.
   *
   * @param factory the document factory to use for creating the new document.
   * @return a new document instance constructed by the given document factory.
   * @throws NullPointerException if the given document factory is null.
   */
  static @NonNull Document.Mutable newDocument(@NonNull DocumentFactory factory) {
    return factory.newDocument();
  }

  /**
   * Get the name of the factory that is able to construct this document type.
   *
   * @return the factory name of this document type.
   */
  @NonNull String factoryName();

  /**
   * Get if the document is empty (therefore has no key-value mappings present).
   *
   * @return true if the document is empty, false otherwise.
   */
  boolean empty();

  /**
   * Get the amount of key-value mappings that are present in the document. Zero indicates that the document is empty.
   *
   * @return the amount of key-value mappings in this document.
   */
  int elementCount();

  /**
   * Get if this document has a value mapping for the given key. Note that the given key is not interpreted as a path.
   *
   * @param key the key to check if a value is present.
   * @return true if this document has a value mapping for the given key, false otherwise.
   * @throws NullPointerException if the given key is null.
   */
  boolean contains(@NonNull String key);

  /**
   * Get if this document has a non-null value mapping for the given key. Note that the given key is not
   * interpreted as a path.
   *
   * @param key the key to check if a value is present.
   * @return true if this document has a non-null value mapping for the given key, false otherwise.
   * @throws NullPointerException if the given key is null.
   */
  boolean containsNonNull(@NonNull String key);

  /**
   * Converts this document into a version that can be cross-handled by other document implementations. The serialized
   * form of this document can be used to convert it to a different type of document. Note that the returned form is a
   * snapshot of the document at the time the method was called. Any changes made to this document after the method call
   * are not reflected into the document send.
   *
   * @return a serialized version of this document.
   * @see Mutable#receive(DocumentSend)
   */
  @NonNull DocumentSend send();

  /**
   * Makes an immutable copy of this document. Changes to this document (in case it is mutable) are not reflected into
   * the returned document and vice-versa.
   *
   * @return an immutable copy of this document.
   */
  @CheckReturnValue
  @NonNull Document immutableCopy();

  /**
   * Makes a mutable copy of this document. Changes to this document (in case it is mutable) are not reflected into the
   * returned document and vice-versa.
   *
   * @return a mutable copy of this document.
   */
  @CheckReturnValue
  @NonNull Document.Mutable mutableCopy();

  /**
   * Get a snapshot view of the keys that are registered in this document at the moment this method was called. New keys
   * that are registered after the method call are not reflecting into the returned collection and vice-versa.
   *
   * @return a view of the registered keys in this document at the moment the method was called.
   */
  @Unmodifiable
  @NonNull Set<String> keys();

  /**
   * Get a snapshot of all serialized key-value pairs registered in this document at the moment this method was called.
   * New pairs that are registered after the method call are not reflecting into the returned collection and
   * vice-versa.
   *
   * @return a view of the registered key-value pairs in this document at the moment the method was called.
   */
  @Unmodifiable
  @NonNull Collection<? extends Element> elements();

  /**
   * Converts the underlying key-value mappings to an instance of the given type. Note that the type must represent an
   * object and cannot be of any other type (like an array or primitive type).
   *
   * @param type the type to construct from the underlying key-value data.
   * @param <T>  the type to model from the underlying data.
   * @return the constructed instance of the given type.
   * @throws NullPointerException if the given type is null.
   */
  @UnknownNullability <T> T toInstanceOf(@NonNull Type type);

  /**
   * Converts the underlying key-value mappings to an instance of the given type. Note that the type must represent an
   * object and cannot be of any other type (like an array or primitive type).
   *
   * @param type the type to construct from the underlying key-value data.
   * @param <T>  the type to model from the underlying data.
   * @return the constructed instance of the given type.
   * @throws NullPointerException if the given type is null.
   */
  @UnknownNullability <T> T toInstanceOf(@NonNull Class<T> type);

  /**
   * Converts the underlying key-value mappings to an instance of the given type. Note that the type must represent an
   * object and cannot be of any other type (like an array or primitive type).
   *
   * @param type the type to construct from the underlying key-value data.
   * @param <T>  the type to model from the underlying data.
   * @return the constructed instance of the given type.
   * @throws NullPointerException if the given type token is null.
   */
  @UnknownNullability <T> T toInstanceOf(@NonNull TypeToken<T> type);

  /**
   * Reads the associated value of the given key and converts it to the given type model. Note that this method is able
   * to read all kinds of types, including arrays, primitives i.a. If an object is requested and a key is missing from
   * the underlying mapping, it is up to the implementation to handle the case. In normal cases a default (fallback)
   * value should get used instead.
   *
   * @param key  the key of the underlying object to convert.
   * @param type the type to convert the underlying data mapping to.
   * @param <T>  the type to model from the underlying data.
   * @return the converted underlying data or null if no value is associated with the given key.
   * @throws NullPointerException if the given key or type is null.
   */
  default @UnknownNullability <T> T readObject(@NonNull String key, @NonNull Type type) {
    return this.readObject(key, type, null);
  }

  /**
   * Reads the associated value of the given key and converts it to the given type model. Note that this method is able
   * to read all kinds of types, including arrays, primitives i.a. If an object is requested and a key is missing from
   * the underlying mapping, it is up to the implementation to handle the case. In normal cases a default (fallback)
   * value should get used instead.
   *
   * @param key  the key of the underlying object to convert.
   * @param type the type to convert the underlying data mapping to.
   * @param <T>  the type to model from the underlying data.
   * @return the converted underlying data or null if no value is associated with the given key.
   * @throws NullPointerException if the given key or type is null.
   */
  default @UnknownNullability <T> T readObject(@NonNull String key, @NonNull Class<T> type) {
    return this.readObject(key, type, null);
  }

  /**
   * Reads the associated value of the given key and converts it to the given type model. Note that this method is able
   * to read all kinds of types, including arrays, primitives i.a. If an object is requested and a key is missing from
   * the underlying mapping, it is up to the implementation to handle the case. In normal cases a default (fallback)
   * value should get used instead.
   *
   * @param key  the key of the underlying object to convert.
   * @param type the type to convert the underlying data mapping to.
   * @param <T>  the type to model from the underlying data.
   * @return the converted underlying data or null if no value is associated with the given key.
   * @throws NullPointerException if the given key or type is null.
   */
  default @UnknownNullability <T> T readObject(@NonNull String key, @NonNull TypeToken<T> type) {
    return this.readObject(key, type, null);
  }

  /**
   * Reads the associated value of the given key and converts it to the given type model. Note that this method is able
   * to read all kinds of types, including arrays, primitives i.a. If an object is requested and a key is missing from
   * the underlying mapping, it is up to the implementation to handle the case. In normal cases a default (fallback)
   * value should get used instead.
   *
   * @param key  the key of the underlying object to convert.
   * @param type the type to convert the underlying data mapping to.
   * @param def  the default value to return if no mapping exists for the given key.
   * @param <T>  the type to model from the underlying data.
   * @return the converted underlying data or the given default value if no value is associated with the given key.
   * @throws NullPointerException if the given key or type is null.
   */
  @UnknownNullability <T> T readObject(@NonNull String key, @NonNull Type type, @Nullable T def);

  /**
   * Reads the associated value of the given key and converts it to the given type model. Note that this method is able
   * to read all kinds of types, including arrays, primitives i.a. If an object is requested and a key is missing from
   * the underlying mapping, it is up to the implementation to handle the case. In normal cases a default (fallback)
   * value should get used instead.
   *
   * @param key  the key of the underlying object to convert.
   * @param type the type to convert the underlying data mapping to.
   * @param def  the default value to return if no mapping exists for the given key.
   * @param <T>  the type to model from the underlying data.
   * @return the converted underlying data or the given default value if no value is associated with the given key.
   * @throws NullPointerException if the given key or type is null.
   */
  @UnknownNullability <T> T readObject(@NonNull String key, @NonNull Class<T> type, @Nullable T def);

  /**
   * Reads the associated value of the given key and converts it to the given type model. Note that this method is able
   * to read all kinds of types, including arrays, primitives i.a. If an object is requested and a key is missing from
   * the underlying mapping, it is up to the implementation to handle the case. In normal cases a default (fallback)
   * value should get used instead.
   *
   * @param key  the key of the underlying object to convert.
   * @param type the type to convert the underlying data mapping to.
   * @param def  the default value to return if no mapping exists for the given key.
   * @param <T>  the type to model from the underlying data.
   * @return the converted underlying data or the given default value if no value is associated with the given key.
   * @throws NullPointerException if the given key or type is null.
   */
  @UnknownNullability <T> T readObject(@NonNull String key, @NonNull TypeToken<T> type, @Nullable T def);

  /**
   * Reads a document of the same type from this document or returns an empty document if no value is associated with
   * the given key. This method never returns null, even if the given key is explicitly associated with null.
   *
   * @param key the key of the underlying document to read.
   * @return a deserialized document of the same type as this document, or an empty document if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  default @NonNull Document readDocument(@NonNull String key) {
    return this.readDocument(key, Document.emptyDocument());
  }

  /**
   * Reads a document of the same type from this document or returns an empty document if no value is associated with
   * the given key. This method returns the given default value, even if the given key is explicitly associated with
   * null.
   *
   * @param key the key of the underlying document to read.
   * @param def the default value to return if the no mapping for the given key exists.
   * @return a deserialized document of the same type as this document, or the given default value if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  @UnknownNullability Document readDocument(@NonNull String key, @Nullable Document def);

  /**
   * Reads a mutable document of the same type from this document or returns an empty document if no value is associated
   * with the given key. This method never returns null, even if the given key is explicitly associated with null.
   *
   * @param key the key of the underlying document to read.
   * @return a deserialized document of the same type as this document, or an empty document if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  default @NonNull Mutable readMutableDocument(@NonNull String key) {
    return this.readMutableDocument(key, Document.emptyDocument());
  }

  /**
   * Reads a mutable document of the same type from this document or returns an empty document if no value is associated
   * with the given key. This method returns the given default value, even if the given key is explicitly associated
   * with null.
   *
   * @param key the key of the underlying document to read.
   * @param def the default value to return if the no mapping for the given key exists.
   * @return a deserialized document of the same type as this document, or the given default value if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  @UnknownNullability Mutable readMutableDocument(@NonNull String key, @Nullable Document.Mutable def);

  /**
   * Reads a byte that is associated with the given key from this document. If no value is associated with the given key
   * or the associated value is not a number then {@code 0} is returned.
   *
   * @param key the key of the byte to read.
   * @return the byte value associated with the given key or {@code 0} if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  default byte getByte(@NonNull String key) {
    return this.getByte(key, (byte) 0);
  }

  /**
   * Reads a short that is associated with the given key from this document. If no value is associated with the given
   * key or the associated value is not a number then {@code 0} is returned.
   *
   * @param key the key of the short to read.
   * @return the short value associated with the given key or {@code 0} if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  default short getShort(@NonNull String key) {
    return this.getShort(key, (short) 0);
  }

  /**
   * Reads an int that is associated with the given key from this document. If no value is associated with the given key
   * or the associated value is not a number then {@code 0} is returned.
   *
   * @param key the key of the int to read.
   * @return the int value associated with the given key or {@code 0} if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  default int getInt(@NonNull String key) {
    return this.getInt(key, 0);
  }

  /**
   * Reads a long that is associated with the given key from this document. If no value is associated with the given key
   * or the associated value is not a number then {@code 0} is returned.
   *
   * @param key the key of the long to read.
   * @return the long value associated with the given key or {@code 0} if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  default long getLong(@NonNull String key) {
    return this.getLong(key, 0);
  }

  /**
   * Reads a float that is associated with the given key from this document. If no value is associated with the given
   * key or the associated value is not a number then {@code 0} is returned.
   *
   * @param key the key of the float to read.
   * @return the float value associated with the given key or {@code 0} if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  default float getFloat(@NonNull String key) {
    return this.getFloat(key, 0);
  }

  /**
   * Reads a double that is associated with the given key from this document. If no value is associated with the given
   * key or the associated value is not a number then {@code 0} is returned.
   *
   * @param key the key of the double to read.
   * @return the double value associated with the given key or {@code 0} if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  default double getDouble(@NonNull String key) {
    return this.getDouble(key, 0);
  }

  /**
   * Reads a boolean that is associated with the given key from this document. If no value is associated with the given
   * key or the associated value is not a number then {@code 0} is returned.
   *
   * @param key the key of the boolean to read.
   * @return the boolean value associated with the given key or {@code 0} if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  default boolean getBoolean(@NonNull String key) {
    return this.getBoolean(key, false);
  }

  /**
   * Reads a string that is associated with the given key from this document. If no value is associated with the given
   * key or the associated value is not a string then null is returned.
   *
   * @param key the key of the string to read.
   * @return the string value associated with the given key or null if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  default @UnknownNullability String getString(@NonNull String key) {
    return this.getString(key, null);
  }

  /**
   * Reads a byte that is associated with the given key from this document. If no value is associated with the given key
   * or the associated value is not a number then the given default value is returned.
   *
   * @param key the key of the byte to read.
   * @param def the default value to return if no mapping exists.
   * @return the byte value associated with the given key or the given default value if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  byte getByte(@NonNull String key, byte def);

  /**
   * Reads a short that is associated with the given key from this document. If no value is associated with the given
   * key or the associated value is not a number then the given default value is returned.
   *
   * @param key the key of the short to read.
   * @param def the default value to return if no mapping exists.
   * @return the short value associated with the given key or the given default value if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  short getShort(@NonNull String key, short def);

  /**
   * Reads an int that is associated with the given key from this document. If no value is associated with the given key
   * or the associated value is not a number then the given default value is returned.
   *
   * @param key the key of the int to read.
   * @param def the default value to return if no mapping exists.
   * @return the int value associated with the given key or the given default value if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  int getInt(@NonNull String key, int def);

  /**
   * Reads a long that is associated with the given key from this document. If no value is associated with the given key
   * or the associated value is not a number then the given default value is returned.
   *
   * @param key the key of the long to read.
   * @param def the default value to return if no mapping exists.
   * @return the long value associated with the given key or the given default value if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  long getLong(@NonNull String key, long def);

  /**
   * Reads a float that is associated with the given key from this document. If no value is associated with the given
   * key or the associated value is not a number then the given default value is returned.
   *
   * @param key the key of the float to read.
   * @param def the default value to return if no mapping exists.
   * @return the float value associated with the given key or the given default value if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  float getFloat(@NonNull String key, float def);

  /**
   * Reads a double that is associated with the given key from this document. If no value is associated with the given
   * key or the associated value is not a number then the given default value is returned.
   *
   * @param key the key of the double to read.
   * @param def the default value to return if no mapping exists.
   * @return the double value associated with the given key or the given default value if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  double getDouble(@NonNull String key, double def);

  /**
   * Reads a boolean that is associated with the given key from this document. If no value is associated with the given
   * key or the associated value is not a number then the given default value is returned.
   *
   * @param key the key of the boolean to read.
   * @param def the default value to return if no mapping exists.
   * @return the boolean value associated with the given key or the given default value if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  boolean getBoolean(@NonNull String key, boolean def);

  /**
   * Reads a string that is associated with the given key from this document. If no value is associated with the given
   * key or the associated value is not a string then the given default value is returned.
   *
   * @param key the key of the string to read.
   * @param def the default value to return if no mapping exists.
   * @return the string value associated with the given key or the given default value if no mapping exists.
   * @throws NullPointerException if the given key is null.
   */
  @UnknownNullability String getString(@NonNull String key, @Nullable String def);

  /**
   * Writes this document pretty into a file at the given path. If one of the parent directory does not exist it gets
   * created as well.
   *
   * @param path the path to write the document to, can be relative or absolute.
   * @throws NullPointerException           if the given path is null.
   * @throws DocumentSerialisationException if the document can't be written to the given path.
   */
  default void writeTo(@NonNull Path path) {
    this.writeTo(path, StandardSerialisationStyle.PRETTY);
  }

  /**
   * Writes this document pretty to the given output stream.
   *
   * @param stream the stream to write the serialized document content to.
   * @throws NullPointerException           if the given stream is null.
   * @throws DocumentSerialisationException if the document can't be written to the given stream.
   */
  default void writeTo(@NonNull OutputStream stream) {
    this.writeTo(stream, StandardSerialisationStyle.PRETTY);
  }

  /**
   * Appends this document pretty to the given appendable.
   *
   * @param appendable the appendable to append the document content to.
   * @throws NullPointerException           if the given appendable is null.
   * @throws DocumentSerialisationException if the document can't be written to the given appendable.
   */
  default void writeTo(@NonNull Appendable appendable) {
    this.writeTo(appendable, StandardSerialisationStyle.PRETTY);
  }

  /**
   * Writes this document compact as a string to the given data buffer.
   *
   * @param dataBuf the data buffer to write the document content to.
   * @throws NullPointerException           if the given data buf is null.
   * @throws DocumentSerialisationException if the document can't be written to the given data buf.
   */
  default void writeTo(@NonNull DataBuf.Mutable dataBuf) {
    this.writeTo(dataBuf, StandardSerialisationStyle.COMPACT);
  }

  /**
   * Serializes this document to a pretty string.
   *
   * @return a pretty serialized string based on this document.
   * @throws DocumentSerialisationException if the document can't be serialized.
   */
  default @NonNull String serializeToString() {
    return this.serializeToString(StandardSerialisationStyle.PRETTY);
  }

  /**
   * Writes this document with the given style into a file at the given path. If one of the parent directory does not
   * exist it gets created as well. Not every custom serialisation style is supported, but at least the standard styles
   * must be supported.
   *
   * @param path  the path to write the document to, can be relative or absolute.
   * @param style the serialization style to use when writing the document.
   * @throws NullPointerException           if the given path or style is null.
   * @throws DocumentSerialisationException if the document can't be written to the given path.
   * @throws UnsupportedOperationException  if the given serialisation style is not supported.
   */
  void writeTo(@NonNull Path path, @NonNull SerialisationStyle style);

  /**
   * Writes this document with the given style to the given output stream. Not every custom serialisation style is
   * supported, but at least the standard styles must be supported.
   *
   * @param stream the stream to write the serialized document content to.
   * @param style  the serialization style to use when writing the document.
   * @throws NullPointerException           if the given stream or style is null.
   * @throws DocumentSerialisationException if the document can't be written to the given stream.
   * @throws UnsupportedOperationException  if the given serialisation style is not supported.
   */
  void writeTo(@NonNull OutputStream stream, @NonNull SerialisationStyle style);

  /**
   * Appends this document with the given style to the given appendable. Not every custom serialisation style is
   * supported, but at least the standard styles must be supported.
   *
   * @param appendable the appendable to write the serialized document content to.
   * @param style      the serialization style to use when writing the document.
   * @throws NullPointerException           if the given appendable or style is null.
   * @throws DocumentSerialisationException if the document can't be written to the given appendable.
   * @throws UnsupportedOperationException  if the given serialisation style is not supported.
   */
  void writeTo(@NonNull Appendable appendable, @NonNull SerialisationStyle style);

  /**
   * Writes this document with the given style as a string to the given data buffer. Not every custom serialisation
   * style is supported, but at least the standard styles must be supported.
   *
   * @param dataBuf the data buffer to write the document content to.
   * @param style   the serialization style to use when writing the document.
   * @throws NullPointerException           if the given data buf or style is null.
   * @throws DocumentSerialisationException if the document can't be written to the given data buf.
   * @throws UnsupportedOperationException  if the given serialisation style is not supported.
   */
  void writeTo(@NonNull DataBuf.Mutable dataBuf, @NonNull SerialisationStyle style);

  /**
   * Serializes this document to a string with the given style. Not every custom serialisation style is supported, but
   * at least the standard styles must be supported.
   *
   * @param style the serialization style to use when writing the document.
   * @return a serialized string with the given style based on this document.
   * @throws NullPointerException           if the given style is null.
   * @throws DocumentSerialisationException if the document can't be serialized.
   * @throws UnsupportedOperationException  if the given serialisation style is not supported.
   */
  @NonNull String serializeToString(@NonNull SerialisationStyle style);

  /**
   * Returns this document compact serialised. This method should only be used for debug reasons, as an api user you
   * should prefer using {@link #serializeToString(SerialisationStyle)} with the {@code COMPACT} standard serialisation
   * style instead.
   *
   * @return a compact serialized string based on this document.
   * @throws DocumentSerialisationException if the document can't be serialized.
   */
  @Override
  @NonNull String toString();

  /**
   * Ensures that the given object is a document of the same type and that all members of this document are present in
   * the given other document. This method will not take entry order into account.
   *
   * @param other the possible other document to check against.
   * @return true if all members of the other document are equal to the members in this document, false otherwise.
   */
  @Override
  boolean equals(@Nullable Object other);

  /**
   * A mutable version of a document. This type allows to set and remove key-value pairs from the underlying
   * implementation but still provides read access to the stored data. Note that this type of document is not required
   * to be thread-safe and users need to be careful to not create data races on accident.
   * <p>
   * For more information how to use the mutable version, see the root documentation of {@link Document}.
   *
   * @since 4.0
   */
  interface Mutable extends Document, DocPropertyHolder.Mutable<Mutable> {

    /**
     * Removes all key-value pairs from this document. After this method call this document is empty.
     *
     * @return the same document instance as used to call the method, for chaining.
     */
    @Contract("-> this")
    @NonNull Document.Mutable clear();

    /**
     * Removes the value associated with the given key from this document. If the key is not associated with any value
     * this method call has no effect.
     *
     * @param key the key to remove from this document.
     * @return the same document instance as used to call the method, for chaining.
     * @throws NullPointerException if the given key is null.
     */
    @Contract("_ -> this")
    @NonNull Document.Mutable remove(@NonNull String key);

    /**
     * Receives the given document send into this document. All key-value pairs given in the document send will be put
     * into this document, ignoring the fact that a key might already be associated with a value in this document. If
     * the document send contains a data type that is not supported by this document type, the key-value pair gets
     * simply skipped.
     * <p>
     * The receiving process of a document send should <strong>never</strong> throw an exception unless the given
     * document send contains malformed or invalid data making it impossible to get imported.
     *
     * @param send the document send to import into this document.
     * @return the same document instance as used to call the method, for chaining.
     * @throws NullPointerException if the given send is null.
     */
    @Contract("_ -> this")
    @NonNull Document.Mutable receive(@NonNull DocumentSend send);

    /**
     * Associates a null value with the given key. If this document type does not support null values, this call has no
     * effect.
     *
     * @param key the key to associate with null.
     * @return the same document instance as used to call the method, for chaining.
     * @throws NullPointerException if the given key is null.
     */
    @Contract("_ -> this")
    @NonNull Document.Mutable appendNull(@NonNull String key);

    /**
     * Appends the key-value pairs based on the given object into this document. Each field of the given object
     * represents a key-value mapping that is directly appended to this document. How the field value is included into
     * this document depends on the type of document. If a field type is not supported by this document type it gets
     * silently ignored. If null or a non-object is passed to this method (for example a primitive) this method call
     * does nothing.
     * <p>
     * Note: passing a generic type to this method will lose all the generic type information.
     *
     * @param value the value to serialize to a tree and append to this document.
     * @return the same document instance as used to call the method, for chaining.
     */
    @Contract("_ -> this")
    @NonNull Document.Mutable appendTree(@Nullable Object value);

    /**
     * Appends all key-value pairs of the given document into this document. If a key-value pair with the same key
     * already exists it gets overridden by this method call. If the given document contains a value type that is not
     * supported by this document, the key-value pair is silently ignored. Changes made to this document after this
     * method call will not reflect into the given document and vice-versa.
     * <p>
     * This method should <strong>never</strong> throw an exception unless the given document contains malformed data.
     *
     * @param document the document to import into this document.
     * @return the same document instance as used to call the method, for chaining.
     * @throws NullPointerException if the given document is null.
     */
    @Contract("_ -> this")
    @NonNull Document.Mutable append(@NonNull Document document);

    /**
     * Appends the key-value pairs based on the given object into this document associated with the given key. Each
     * field of the given object represents a key-value mapping. How the field value is included into this document
     * depends on the type of document. If a field type is not supported by this document type it gets silently
     * ignored.
     * <p>
     * Note: passing a generic type to this method will lose all the generic type information.
     *
     * @param key   the key to associate the given value with.
     * @param value the value to serialize to a tree and append to this document associated with the given key.
     * @return the same document instance as used to call the method, for chaining.
     * @throws NullPointerException if the given key is null.
     */
    @Contract("_, _ -> this")
    @NonNull Document.Mutable append(@NonNull String key, @Nullable Object value);

    /**
     * Appends the given number associated with the given key to this document. If the passed number instance is null,
     * then null is appended to this document. If this document does not support numbers (or nulls) then this method
     * call has no effect.
     *
     * @param key   the key to associate the given number with.
     * @param value the number value to associate with the given key.
     * @return the same document instance as used to call the method, for chaining.
     * @throws NullPointerException if the given key is null.
     */
    @Contract("_, _ -> this")
    @NonNull Document.Mutable append(@NonNull String key, @Nullable Number value);

    /**
     * Appends the given boolean associated with the given key to this document. If the passed boolean instance is null,
     * then null is appended to this document. If this document does not support booleans (or nulls) then this method
     * call has no effect.
     *
     * @param key   the key to associate the given boolean with.
     * @param value the boolean value to associate with the given key.
     * @return the same document instance as used to call the method, for chaining.
     * @throws NullPointerException if the given key is null.
     */
    @Contract("_, _ -> this")
    @NonNull Document.Mutable append(@NonNull String key, @Nullable Boolean value);

    /**
     * Appends the given string associated with the given key to this document. If the passed string instance is null,
     * then null is appended to this document. If this document does not support strings (or nulls) then this method
     * call has no effect.
     *
     * @param key   the key to associate the given string with.
     * @param value the string value to associate with the given key.
     * @return the same document instance as used to call the method, for chaining.
     * @throws NullPointerException if the given key is null.
     */
    @Contract("_, _ -> this")
    @NonNull Document.Mutable append(@NonNull String key, @Nullable String value);

    /**
     * Appends the given document associated with the given key to this document. If the passed document instance is
     * null, then null is appended to this document. If the given document contains a value type that is not supported
     * by this document then that key-value pair is ignored.
     * <p>
     * This method should <strong>never</strong> throw an exception unless the given document contains malformed data.
     *
     * @param key   the key to associate the given document with.
     * @param value the document value to associate with the given key.
     * @return the same document instance as used to call the method, for chaining.
     * @throws NullPointerException if the given key is null.
     */
    @Contract("_, _ -> this")
    @NonNull Document.Mutable append(@NonNull String key, @Nullable Document value);
  }
}
