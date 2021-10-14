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

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.defaults.DefaultSpecificTemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SpecificTemplateStorage extends INameable {

  /**
   * Creates a new {@link SpecificTemplateStorage} for the given template.
   *
   * @param template the template which should be wrapped by the given storage
   * @return a new instance of the {@link SpecificTemplateStorage}
   * @throws IllegalArgumentException if the storage of the given template doesn't exist
   */
  @NotNull
  static SpecificTemplateStorage of(@NotNull ServiceTemplate template) {
    return DefaultSpecificTemplateStorage.of(template);
  }

  /**
   * Creates a new {@link SpecificTemplateStorage} for the given template.
   *
   * @param template the template which should be wrapped by the given storage
   * @param storage  the {@link TemplateStorage} instance matching the name of the storage in the template
   * @return a new instance of the {@link SpecificTemplateStorage}
   * @throws IllegalArgumentException if the name of the storage doesn't match the name of the storage in the template
   */
  @NotNull
  static SpecificTemplateStorage of(@NotNull ServiceTemplate template, @NotNull TemplateStorage storage) {
    return DefaultSpecificTemplateStorage.of(template, storage);
  }

  /**
   * Gets the template this class is wrapping.
   */
  @NotNull
  ServiceTemplate getTargetTemplate();

  /**
   * Gets the storage this class is wrapping.
   */
  @NotNull
  TemplateStorage getWrappedStorage();

  /**
   * @see TemplateStorage#deploy(Path, ServiceTemplate, Predicate)
   */
  boolean deploy(@NotNull Path directory, @Nullable Predicate<Path> fileFilter);

  /**
   * @see TemplateStorage#deploy(Path, ServiceTemplate)
   */
  default boolean deploy(@NotNull Path directory) {
    return this.deploy(directory, null);
  }

  /**
   * @see TemplateStorage#deploy(InputStream, ServiceTemplate)
   */
  boolean deploy(@NotNull InputStream inputStream);

  /**
   * @see TemplateStorage#copy(ServiceTemplate, Path)
   */
  boolean copy(@NotNull Path directory);

  /**
   * @see TemplateStorage#asZipInputStream(ServiceTemplate)
   */
  @Nullable
  ZipInputStream asZipInputStream() throws IOException;

  /**
   * @see TemplateStorage#zipTemplate(ServiceTemplate)
   */
  @Nullable
  InputStream zipTemplate() throws IOException;

  /**
   * @see TemplateStorage#delete(ServiceTemplate)
   */
  boolean delete();

  /**
   * @see TemplateStorage#create(ServiceTemplate)
   */
  boolean create();

  /**
   * @see TemplateStorage#has(ServiceTemplate)
   */
  boolean exists();

  /**
   * @see TemplateStorage#appendOutputStream(ServiceTemplate, String)
   */
  @Nullable
  OutputStream appendOutputStream(@NotNull String path) throws IOException;

  /**
   * @see TemplateStorage#newOutputStream(ServiceTemplate, String)
   */
  @Nullable
  OutputStream newOutputStream(@NotNull String path) throws IOException;

  /**
   * @see TemplateStorage#createFile(ServiceTemplate, String)
   */
  boolean createFile(@NotNull String path) throws IOException;

  /**
   * @see TemplateStorage#createDirectory(ServiceTemplate, String)
   */
  boolean createDirectory(@NotNull String path) throws IOException;

  /**
   * @see TemplateStorage#hasFile(ServiceTemplate, String)
   */
  boolean hasFile(@NotNull String path) throws IOException;

  /**
   * @see TemplateStorage#deleteFile(ServiceTemplate, String)
   */
  boolean deleteFile(@NotNull String path) throws IOException;

  /**
   * @see TemplateStorage#newInputStream(ServiceTemplate, String)
   */
  @Nullable
  InputStream newInputStream(@NotNull String path) throws IOException;

  /**
   * @see TemplateStorage#getFileInfo(ServiceTemplate, String)
   */
  @Nullable
  FileInfo getFileInfo(@NotNull String path) throws IOException;

  /**
   * @see TemplateStorage#listFiles(ServiceTemplate, String, boolean)
   */
  @Nullable
  FileInfo[] listFiles(@NotNull String dir, boolean deep) throws IOException;

  /**
   * @see TemplateStorage#listFiles(ServiceTemplate, boolean)
   */
  @Nullable
  default FileInfo[] listFiles(boolean deep) throws IOException {
    return this.listFiles("", deep);
  }

  /**
   * @see TemplateStorage#listFiles(ServiceTemplate, String)
   */
  @Nullable
  default FileInfo[] listFiles(@NotNull String dir) throws IOException {
    return this.listFiles(dir, true);
  }

  /**
   * @see TemplateStorage#listFiles(ServiceTemplate)
   */
  @Nullable
  default FileInfo[] listFiles() throws IOException {
    return this.listFiles(true);
  }

  /**
   * @see TemplateStorage#deployAsync(Path, ServiceTemplate, Predicate)
   */
  @NotNull
  ITask<Boolean> deployAsync(@NotNull Path directory, @Nullable Predicate<Path> fileFilter);

  /**
   * @see TemplateStorage#deployAsync(Path, ServiceTemplate)
   */
  @NotNull
  default ITask<Boolean> deployAsync(@NotNull Path directory) {
    return this.deployAsync(directory, null);
  }

  /**
   * @see TemplateStorage#deployAsync(InputStream, ServiceTemplate)
   */
  @NotNull
  ITask<Boolean> deployAsync(@NotNull InputStream inputStream);

  /**
   * @see TemplateStorage#copyAsync(ServiceTemplate, Path)
   */
  @NotNull
  ITask<Boolean> copyAsync(@NotNull Path directory);

  /**
   * @see TemplateStorage#asZipInputStreamAsync(ServiceTemplate)
   */
  @NotNull
  ITask<ZipInputStream> asZipInputStreamAsync();

  /**
   * @see TemplateStorage#zipTemplateAsync(ServiceTemplate)
   */
  @NotNull
  ITask<InputStream> zipTemplateAsync();

  /**
   * @see TemplateStorage#deleteAsync(ServiceTemplate)
   */
  @NotNull
  ITask<Boolean> deleteAsync();

  /**
   * @see TemplateStorage#createAsync(ServiceTemplate)
   */
  @NotNull
  ITask<Boolean> createAsync();

  /**
   * @see TemplateStorage#hasAsync(ServiceTemplate)
   */
  @NotNull
  ITask<Boolean> existsAsync();

  /**
   * @see TemplateStorage#appendOutputStreamAsync(ServiceTemplate, String)
   */
  @NotNull
  ITask<OutputStream> appendOutputStreamAsync(@NotNull String path);

  /**
   * @see TemplateStorage#newOutputStreamAsync(ServiceTemplate, String)
   */
  @NotNull
  ITask<OutputStream> newOutputStreamAsync(@NotNull String path);

  /**
   * @see TemplateStorage#createFileAsync(ServiceTemplate, String)
   */
  @NotNull
  ITask<Boolean> createFileAsync(@NotNull String path);

  /**
   * @see TemplateStorage#createDirectoryAsync(ServiceTemplate, String)
   */
  @NotNull
  ITask<Boolean> createDirectoryAsync(@NotNull String path);

  /**
   * @see TemplateStorage#hasFileAsync(ServiceTemplate, String)
   */
  @NotNull
  ITask<Boolean> hasFileAsync(@NotNull String path);

  /**
   * @see TemplateStorage#deleteFileAsync(ServiceTemplate, String)
   */
  @NotNull
  ITask<Boolean> deleteFileAsync(@NotNull String path);

  /**
   * @see TemplateStorage#newInputStreamAsync(ServiceTemplate, String)
   */
  @NotNull
  ITask<InputStream> newInputStreamAsync(@NotNull String path);

  /**
   * @see TemplateStorage#getFileInfoAsync(ServiceTemplate, String)
   */
  @NotNull
  ITask<FileInfo> getFileInfoAsync(@NotNull String path);

  /**
   * @see TemplateStorage#listFilesAsync(ServiceTemplate, String, boolean)
   */
  @NotNull
  ITask<FileInfo[]> listFilesAsync(@NotNull String dir, boolean deep);

  /**
   * @see TemplateStorage#listFilesAsync(ServiceTemplate, boolean)
   */
  @NotNull
  default ITask<FileInfo[]> listFilesAsync(boolean deep) {
    return this.listFilesAsync("", deep);
  }

  /**
   * @see TemplateStorage#listFilesAsync(ServiceTemplate, String)
   */
  @NotNull
  default ITask<FileInfo[]> listFilesAsync(@NotNull String dir) {
    return this.listFilesAsync(dir, true);
  }

  /**
   * @see TemplateStorage#listFilesAsync(ServiceTemplate)
   */
  @NotNull
  default ITask<FileInfo[]> listFilesAsync() {
    return this.listFilesAsync(true);
  }

}
