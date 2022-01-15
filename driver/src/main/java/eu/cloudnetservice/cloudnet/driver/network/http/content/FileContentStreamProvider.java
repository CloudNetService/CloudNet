/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.network.http.content;

import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.driver.util.FileMimeTypeHelper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A provider which loads the stream content from a file in the given working directory, while preventing any kind of
 * path traversal.
 *
 * @param workingDirectory the directory to lookup requested paths in.
 * @since 4.0
 */
record FileContentStreamProvider(@NonNull Path workingDirectory) implements ContentStreamProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable StreamableContent provideContent(@NonNull String path) {
    var targetPath = this.workingDirectory.resolve(path);
    if (Files.notExists(targetPath) || Files.isDirectory(targetPath)) {
      return null;
    } else {
      FileUtil.ensureChild(this.workingDirectory, targetPath);
      return new FileStreamableContent(targetPath);
    }
  }

  /**
   * A stream source wrapping a specific file.
   *
   * @since 4.0
   */
  private static final class FileStreamableContent implements StreamableContent {

    private final Path path;
    private final String contentType;

    /**
     * Constructs a new file streamable content instance.
     *
     * @param path the path to the wrapped content.
     * @throws NullPointerException if the given path is null.
     */
    public FileStreamableContent(@NonNull Path path) {
      this.path = path;
      this.contentType = FileMimeTypeHelper.fileType(path) + "; charset=UTF-8";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull InputStream openStream() throws IOException {
      return Files.newInputStream(this.path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull String contentType() {
      return this.contentType;
    }
  }
}
