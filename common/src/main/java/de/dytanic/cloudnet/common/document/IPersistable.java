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

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface is interesting to persistence data, of the implement object
 */
public interface IPersistable {

  Logger LOGGER = LogManager.getLogger(IPersistable.class);

  @NotNull IPersistable write(@NotNull Writer writer);

  default @NotNull IPersistable write(@NotNull OutputStream outputStream) {
    try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      return this.write(writer);
    } catch (IOException exception) {
      LOGGER.severe("Exception while writing output stream", exception);
      return this;
    }
  }

  default @NotNull IPersistable write(@Nullable Path path) {
    if (path != null) {
      // ensure that the parent directory exists
      FileUtils.createDirectory(path.getParent());
      // write to the file
      try (OutputStream stream = Files.newOutputStream(path)) {
        return this.write(stream);
      } catch (IOException exception) {
        LOGGER.severe("Exception while writing document to " + path, exception);
      }
    }
    return this;
  }
}
