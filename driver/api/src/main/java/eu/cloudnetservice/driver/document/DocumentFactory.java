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

import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A factory for documents. Documents can be created from various input sources as well as completely empty. A factory
 * can only produce documents for a single type.
 *
 * @since 4.0
 */
public interface DocumentFactory {

  /**
   * Gets the jvm static document factory for json documents.
   *
   * @return the jvm static document factory for json documents.
   */
  static @NonNull DocumentFactory json() {
    return ServiceRegistry.registry().instance(DocumentFactory.class, "json");
  }

  /**
   * Get the name of the format that this factory produces documents for.
   *
   * @return the name of the format this factory produces documents for.
   */
  @NonNull String formatName();

  /**
   * Parses a document of the factory supported document type from the given input data. The given data must be the root
   * object of a document in order to work. If possible it should be preferred to read the data from a stream to prevent
   * large heap allocations.
   *
   * @param data the data to parse the document from.
   * @return a parsed document from the given input data.
   * @throws DocumentParseException if the document cannot be parsed from the given data.
   */
  @NonNull Document.Mutable parse(byte[] data);

  /**
   * Parses a document of the factory supported document type from the file at the given path. The given data must be
   * the root object of a document in order to work. Note: if the file at the given does not exist or the given path is
   * a directory, this method returns a new, empty document from this factory (as specified by {@link #newDocument()}).
   * However, this method invocation will fail in case the current jvm does not have the appropriate privileges that
   * would allow it open the file for reading.
   *
   * @param path the path to the file to parse.
   * @return a parsed document from the file at the given path.
   * @throws NullPointerException   if the given path is null.
   * @throws DocumentParseException if the document cannot be parsed from the given path.
   */
  @NonNull Document.Mutable parse(@NonNull Path path);

  /**
   * Parses a document of the factory supported document type from the given string. The given data must be the root
   * object of a document in order to work. If possible it should be preferred to read the data from a stream to prevent
   * large heap allocations.
   *
   * @param data the string data to read the document from.
   * @return a parsed document from the given data.
   * @throws NullPointerException   if the given data is null.
   * @throws DocumentParseException if the document cannot be parsed from the given data.
   */
  @NonNull Document.Mutable parse(@NonNull String data);

  /**
   * Parses a document of the factory supported document type from the given reader. The given data must be the root
   * object of a document in order to work.
   *
   * @param reader a reader of the data to read the document from.
   * @return a parsed document from the given data.
   * @throws NullPointerException   if the given reader is null.
   * @throws DocumentParseException if the document cannot be parsed from the given data.
   */
  @NonNull Document.Mutable parse(@NonNull Reader reader);

  /**
   * Parses a document of the factory supported document type from the given input stream. The given data must be the
   * root object of a document in order to work.
   *
   * @param stream a stream of the data to read the document from.
   * @return a parsed document from the given data.
   * @throws NullPointerException   if the given stream is null.
   * @throws DocumentParseException if the document cannot be parsed from the given data.
   */
  @NonNull Document.Mutable parse(@NonNull InputStream stream);

  /**
   * Parses a document of the factory supported document type from the given data buf. The given data must be the root
   * object of a document in order to work. This method will read one string from the given buffer and parse that.
   *
   * @param dataBuf the data buf to read a string to parse from.
   * @return a parsed document from the data in the given buffer.
   * @throws NullPointerException   if the given data buf is null.
   * @throws DocumentParseException if the document cannot be parsed from the given data.
   */
  @NonNull Document.Mutable parse(@NonNull DataBuf dataBuf);

  /**
   * Creates a new, completely empty document of the factory supported document type.
   *
   * @return a new, completely empty document.
   */
  @NonNull Document.Mutable newDocument();

  /**
   * Creates a new, completely empty document and appends the tree of the given object to the document. See
   * {@link Document.Mutable#appendTree(Object)} for more information. If the given initial object is null, this method
   * does the same as {@link #newDocument()}.
   *
   * @param wrapped the object to initially append the tree of.
   * @return a new document with the tree of the given object appended.
   */
  @NonNull Document.Mutable newDocument(@Nullable Object wrapped);

  /**
   * Creates a new, completely empty document and appends the given key-value pair initially to it. If the given initial
   * value is null then null will be associated with the given key unless null values are not supported by this document
   * type.
   *
   * @param key   the key of the initial mapping.
   * @param value the value of the initial mapping.
   * @return a new document with the given key-value pair initially appended.
   * @throws NullPointerException if the given key is null.
   */
  @NonNull Document.Mutable newDocument(@NonNull String key, @Nullable Object value);

  /**
   * Receives the given document send into a new, completely empty document. All key-value pairs given in the document
   * send will be put into this document, ignoring the fact that a key might already be associated with a value in this
   * document. If the document send contains a data type that is not supported by this document type, the key-value pair
   * gets simply skipped.
   * <p>
   * The receiving process of a document send should <strong>never</strong> throw an exception unless the given document
   * send contains malformed or invalid data making it impossible to get imported.
   *
   * @param send the document send to import into this document.
   * @return a new, empty document containing all supported key-value pairs of the given document send.
   * @throws NullPointerException if the given send is null.
   */
  @NonNull Document.Mutable receive(@NonNull DocumentSend send);
}
