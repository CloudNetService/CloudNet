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

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface is interesting to persistence data, of the implement object
 */
public interface Persistable {

  Logger LOGGER = LogManager.logger(Persistable.class);

  @NonNull Persistable write(@NonNull Writer writer);

  default @NonNull Persistable write(@NonNull OutputStream outputStream) {
    try (var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      return this.write(writer);
    } catch (IOException exception) {
      LOGGER.severe("Exception while writing output stream", exception);
      return this;
    }
  }

  default @NonNull Persistable write(@Nullable Path path) {
    if (path != null) {
      // ensure that the parent directory exists
      FileUtil.createDirectory(path.getParent());
      // write to the file
      try (var stream = Files.newOutputStream(path)) {
        return this.write(stream);
      } catch (IOException exception) {
        LOGGER.severe("Exception while writing document to " + path, exception);
      }
    }
    return this;
  }
}
