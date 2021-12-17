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

package de.dytanic.cloudnet.driver.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record FileInfo(
  @NotNull String path,
  @NotNull String name,
  boolean directory,
  boolean hidden,
  long creationTime,
  long lastModified,
  long lastAccess,
  long size
) {

  @NotNull
  public static FileInfo of(@NotNull Path path) throws IOException {
    return of(path, (Path) null);
  }

  @NotNull
  public static FileInfo of(@NotNull Path fullPath, @NotNull BasicFileAttributes attributes) throws IOException {
    return of(fullPath, null, attributes);
  }

  @NotNull
  public static FileInfo of(@NotNull Path path, @Nullable Path relativePath) throws IOException {
    return of(path, relativePath, Files.readAttributes(path, BasicFileAttributes.class));
  }

  @NotNull
  public static FileInfo of(
    @NotNull Path fullPath,
    @Nullable Path relativePath,
    @NotNull BasicFileAttributes attributes
  ) throws IOException {
    if (relativePath == null) {
      relativePath = fullPath;
    }

    return new FileInfo(
      relativePath.toString().replace(File.separatorChar, '/'),
      relativePath.getFileName().toString(),
      attributes.isDirectory(),
      Files.isHidden(fullPath),
      attributes.creationTime().toMillis(),
      attributes.lastModifiedTime().toMillis(),
      attributes.lastAccessTime().toMillis(),
      attributes.size());
  }

  @NotNull
  public static FileInfo of(@NotNull File file) throws IOException {
    return of(file.toPath(), file.toPath());
  }
}

