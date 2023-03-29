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

package eu.cloudnetservice.driver.document.empty;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class EmptyDocumentFactory implements DocumentFactory {

  public static final DocumentFactory INSTANCE = new EmptyDocumentFactory();

  private EmptyDocumentFactory() {
  }

  @Override
  public @NonNull String formatName() {
    return "empty";
  }

  @Override
  public @NonNull Document.Mutable parse(byte[] data) {
    return Document.emptyDocument();
  }

  @Override
  public @NonNull Document.Mutable parse(@NonNull Path path) {
    return Document.emptyDocument();
  }

  @Override
  public @NonNull Document.Mutable parse(@NonNull String data) {
    return Document.emptyDocument();
  }

  @Override
  public @NonNull Document.Mutable parse(@NonNull Reader reader) {
    return Document.emptyDocument();
  }

  @Override
  public @NonNull Document.Mutable parse(@NonNull InputStream stream) {
    return Document.emptyDocument();
  }

  @Override
  public @NonNull Document.Mutable parse(@NonNull DataBuf dataBuf) {
    return Document.emptyDocument();
  }

  @Override
  public @NonNull Document.Mutable newDocument() {
    return Document.emptyDocument();
  }

  @Override
  public @NonNull Document.Mutable newDocument(@Nullable Object wrapped) {
    return Document.emptyDocument();
  }

  @Override
  public @NonNull Document.Mutable newDocument(@NonNull String key, @Nullable Object value) {
    return Document.emptyDocument();
  }

  @Override
  public @NonNull Document.Mutable receive(@NonNull DocumentSend send) {
    return Document.emptyDocument();
  }
}
