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

package eu.cloudnetservice.cloudnet.driver.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a file which is stored on any file system which is accessible.
 *
 * @param path         the relative of path of the file on the current file system.
 * @param name         the name of the file.
 * @param directory    if this information represents a directory.
 * @param hidden       if the file or directory is hidden.
 * @param creationTime a unix timestamp which represents the creation time of the file.
 * @param lastModified a unix timestamp which represents the last time the file was changed.
 * @param lastAccess   a unix timestamp which represents the last time the file was accessed.
 * @param size         the size of the file, in bytes.
 * @since 4.0
 */
public record FileInfo(
  @NonNull String path,
  @NonNull String name,
  boolean directory,
  boolean hidden,
  long creationTime,
  long lastModified,
  long lastAccess,
  long size
) {

  /**
   * Constructs a new file information for the given path.
   *
   * @param path the path to the file to construct the info for.
   * @return the created file information based on the given path.
   * @throws IOException          if an i/o error occurs while accessing the file attributes of the given path.
   * @throws NullPointerException if the given path is null.
   */
  public static @NonNull FileInfo of(@NonNull Path path) throws IOException {
    return of(path, (Path) null);
  }

  /**
   * Constructs a new file information for the given path and the given file attributes.
   *
   * @param path       the path to the file to construct the info for.
   * @param attributes the attributes of the file to use when constructing the file info.
   * @return the created file information based on the given path and file attributes.
   * @throws IOException          if an i/o error occurs while accessing the file attributes of the given path.
   * @throws NullPointerException if either the given path or file attribute information is null.
   */
  public static @NonNull FileInfo of(@NonNull Path path, @NonNull BasicFileAttributes attributes) throws IOException {
    return of(path, null, attributes);
  }

  /**
   * Constructs a new file information for the given path. The file path in the resulting information will be relative
   * if the relative path is given.
   *
   * @param path         the full path of the file to create the information for.
   * @param relativePath the relative path to the file to create the information for.
   * @return the created file information based on the given path and relative path (if present).
   * @throws IOException          if an i/o error occurs while accessing the file attributes of the given path.
   * @throws NullPointerException if the given file path is null.
   */
  public static @NonNull FileInfo of(@NonNull Path path, @Nullable Path relativePath) throws IOException {
    return of(path, relativePath, Files.readAttributes(path, BasicFileAttributes.class));
  }

  /**
   * Creates a new file information for the given path and the given file attributes. If the given relative path is
   * present it will be used as the path in the created file information.
   *
   * @param fullPath     the full path to the file to create the information for.
   * @param relativePath the relative path to the file to create the information for.
   * @param attributes   the attributes of the file to use when constructing the file info.
   * @return the created file information based on the given file path, file attributes and the relative path.
   * @throws IOException          if an i/o error occurs while accessing the file attributes of the given path.
   * @throws NullPointerException if either the given file path or file attribute information is null.
   */
  public static @NonNull FileInfo of(
    @NonNull Path fullPath,
    @Nullable Path relativePath,
    @NonNull BasicFileAttributes attributes
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
}
