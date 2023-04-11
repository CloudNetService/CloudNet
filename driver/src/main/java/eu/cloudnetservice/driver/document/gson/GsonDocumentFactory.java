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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.DocumentParseException;
import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A document factory for json documents based on gson. The format name of this factory is {@code json}.
 *
 * @since 4.0
 */
public final class GsonDocumentFactory implements DocumentFactory {

  /**
   * The singleton instance of this document factory. External api users should not depend on this field and use
   * {@link DocumentFactory#json()} instead.
   */
  public static final DocumentFactory INSTANCE = new GsonDocumentFactory();

  /**
   * Sealed constructor as there should only be one singleton gson document factory.
   */
  private GsonDocumentFactory() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String formatName() {
    return "json";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(byte[] data) {
    try (var stream = new ByteArrayInputStream(data)) {
      return this.parse(stream);
    } catch (IOException exception) {
      throw new DocumentParseException(exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(@NonNull Path path) {
    try (var stream = Files.newInputStream(path)) {
      return this.parse(stream);
    } catch (IOException exception) {
      throw new DocumentParseException("Unable to parse document from path " + path, exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(@NonNull String data) {
    try (var reader = new StringReader(data)) {
      return this.parse(reader);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(@NonNull Reader reader) {
    try {
      var element = JsonParser.parseReader(reader);
      return element.isJsonObject() ? new MutableGsonDocument(element.getAsJsonObject()) : this.newDocument();
    } catch (JsonParseException exception) {
      throw new DocumentParseException(exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(@NonNull InputStream stream) {
    try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      return this.parse(reader);
    } catch (IOException exception) {
      throw new DocumentParseException(exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(@NonNull DataBuf dataBuf) {
    return this.parse(dataBuf.readString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable newDocument() {
    return new MutableGsonDocument();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable newDocument(@Nullable Object wrapped) {
    var element = GsonProvider.NORMAL_GSON_INSTANCE.toJsonTree(wrapped);
    return element.isJsonObject() ? new MutableGsonDocument(element.getAsJsonObject()) : this.newDocument();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable newDocument(@NonNull String key, @Nullable Object value) {
    // serialize the value
    var element = GsonProvider.NORMAL_GSON_INSTANCE.toJsonTree(value);

    // put in the element
    var object = new JsonObject();
    object.add(key, element);
    return new MutableGsonDocument(object);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable receive(@NonNull DocumentSend send) {
    return this.newDocument().receive(send);
  }
}
