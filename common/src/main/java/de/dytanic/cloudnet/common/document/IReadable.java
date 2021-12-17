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

package de.dytanic.cloudnet.common.document;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface is interesting to read data, of the implement object
 */
public interface IReadable {

  Logger LOGGER = LogManager.logger(IReadable.class);

  @NotNull IReadable read(@NotNull Reader reader);

  default @NotNull IReadable read(@NotNull InputStream inputStream) {
    try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      return this.read(reader);
    } catch (IOException exception) {
      LOGGER.severe("Exception while parsing document from input stream", exception);
      return this;
    }
  }

  default @NotNull IReadable read(@Nullable Path path) {
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
