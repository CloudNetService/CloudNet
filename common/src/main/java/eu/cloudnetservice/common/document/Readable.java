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

package eu.cloudnetservice.common.document;

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface is interesting to read data, of the implement object
 */
public interface Readable {

  Logger LOGGER = LogManager.logger(Readable.class);

  @NonNull Readable read(@NonNull Reader reader);

  default @NonNull Readable read(@NonNull InputStream inputStream) {
    try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      return this.read(reader);
    } catch (IOException exception) {
      LOGGER.severe("Exception while parsing document from input stream", exception);
      return this;
    }
  }

  default @NonNull Readable read(@Nullable Path path) {
    if (path != null && Files.exists(path)) {
      try (var inputStream = Files.newInputStream(path)) {
        return this.read(inputStream);
      } catch (IOException exception) {
        LOGGER.severe("Exception while reading document from " + path, exception);
      }
    }
    return this;
  }
}
