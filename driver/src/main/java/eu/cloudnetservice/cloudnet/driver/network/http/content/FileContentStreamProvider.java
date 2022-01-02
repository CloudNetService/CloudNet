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

import eu.cloudnetservice.cloudnet.common.io.FileUtils;
import eu.cloudnetservice.cloudnet.driver.util.FileMimeTypeHelper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

record FileContentStreamProvider(@NonNull Path workingDirectory) implements ContentStreamProvider {

  @Override
  public @Nullable StreamableContent provideContent(@NonNull String path) {
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
      this.contentType = FileMimeTypeHelper.fileType(path) + "; charset=UTF-8";
    }

    @Override
    public @NonNull InputStream openStream() throws IOException {
      return Files.newInputStream(this.path);
    }

    @Override
    public @NonNull String contentType() {
      return this.contentType;
    }
  }
}
