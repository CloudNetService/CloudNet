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

package de.dytanic.cloudnet.driver.network.http.content;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.util.FileMimeTypeHelper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class FileContentStreamProvider implements ContentStreamProvider {

  private final Path workingDirectory;

  public FileContentStreamProvider(Path workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  @Override
  public @Nullable StreamableContent provideContent(@NotNull String path) {
    var targetPath = this.workingDirectory.resolve(path);
    if (Files.notExists(targetPath) || Files.isDirectory(targetPath)) {
      return null;
    } else {
      FileUtils.ensureChild(this.workingDirectory, targetPath);
      return new FileStreamableContent(targetPath);
    }
  }

  private static final class FileStreamableContent implements StreamableContent {

    private final Path path;
    private final String contentType;

    public FileStreamableContent(Path path) {
      this.path = path;
      this.contentType = FileMimeTypeHelper.getFileType(path) + "; charset=UTF-8";
    }

    @Override
    public @NotNull InputStream openStream() throws IOException {
      return Files.newInputStream(this.path);
    }

    @Override
    public @NotNull String contentType() {
      return this.contentType;
    }
  }
}
