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

package eu.cloudnetservice.driver.impl.document.empty;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.registry.AutoService;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A document factory that only returns the singleton empty document instance. The format name of this factory is
 * {@code empty}.
 *
 * @since 4.0
 */
@Singleton
@AutoService(services = DocumentFactory.class, name = "empty")
public final class EmptyDocumentFactory implements DocumentFactory {

  /**
   * Sealed constructor as there should only be one singleton empty document factory.
   */
  public EmptyDocumentFactory() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String formatName() {
    return "empty";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(byte[] data) {
    return EmptyDocument.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(@NonNull Path path) {
    return EmptyDocument.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(@NonNull String data) {
    return EmptyDocument.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(@NonNull Reader reader) {
    return EmptyDocument.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(@NonNull InputStream stream) {
    return EmptyDocument.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable parse(@NonNull DataBuf dataBuf) {
    return EmptyDocument.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable newDocument() {
    return EmptyDocument.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable newDocument(@Nullable Object wrapped) {
    return EmptyDocument.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable newDocument(@NonNull String key, @Nullable Object value) {
    return EmptyDocument.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable receive(@NonNull DocumentSend send) {
    return EmptyDocument.INSTANCE;
  }
}
