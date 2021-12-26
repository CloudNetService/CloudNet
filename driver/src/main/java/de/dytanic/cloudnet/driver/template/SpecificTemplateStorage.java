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

import de.dytanic.cloudnet.common.Nameable;
import de.dytanic.cloudnet.common.concurrent.Task;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.defaults.DefaultSpecificTemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface SpecificTemplateStorage extends Nameable {

  /**
   * Creates a new {@link SpecificTemplateStorage} for the given template.
   *
   * @param template the template which should be wrapped by the given storage
   * @return a new instance of the {@link SpecificTemplateStorage}
   * @throws IllegalArgumentException if the storage of the given template doesn't exist
   */
  @NonNull
  static SpecificTemplateStorage of(@NonNull ServiceTemplate template) {
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
  @NonNull
  static SpecificTemplateStorage of(@NonNull ServiceTemplate template, @NonNull TemplateStorage storage) {
    return DefaultSpecificTemplateStorage.of(template, storage);
  }

  /**
   * Gets the template this class is wrapping.
   */
  @NonNull
  ServiceTemplate targetTemplate();

  /**
   * Gets the storage this class is wrapping.
   */
  @NonNull
  TemplateStorage wrappedStorage();

  /**
   * @see TemplateStorage#deployDirectory(Path, ServiceTemplate, Predicate)
   */
  boolean deploy(@NonNull Path directory, @Nullable Predicate<Path> fileFilter);

  /**
   * @see TemplateStorage#deployDirectory(Path, ServiceTemplate)
   */
  default boolean deploy(@NonNull Path directory) {
    return this.deploy(directory, null);
  }

  /**
   * @see TemplateStorage#deploy(InputStream, ServiceTemplate)
   */
  boolean deploy(@NonNull InputStream inputStream);

  /**
   * @see TemplateStorage#copy(ServiceTemplate, Path)
   */
  boolean copy(@NonNull Path directory);

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
  OutputStream appendOutputStream(@NonNull String path) throws IOException;

  /**
   * @see TemplateStorage#newOutputStream(ServiceTemplate, String)
   */
  @Nullable
  OutputStream newOutputStream(@NonNull String path) throws IOException;

  /**
   * @see TemplateStorage#createFile(ServiceTemplate, String)
   */
  boolean createFile(@NonNull String path) throws IOException;

  /**
   * @see TemplateStorage#createDirectory(ServiceTemplate, String)
   */
  boolean createDirectory(@NonNull String path) throws IOException;

  /**
   * @see TemplateStorage#hasFile(ServiceTemplate, String)
   */
  boolean hasFile(@NonNull String path) throws IOException;

  /**
   * @see TemplateStorage#deleteFile(ServiceTemplate, String)
   */
  boolean deleteFile(@NonNull String path) throws IOException;

  /**
   * @see TemplateStorage#newInputStream(ServiceTemplate, String)
   */
  @Nullable
  InputStream newInputStream(@NonNull String path) throws IOException;

  /**
   * @see TemplateStorage#fileInfo(ServiceTemplate, String)
   */
  @Nullable
  FileInfo fileInfo(@NonNull String path) throws IOException;

  /**
   * @see TemplateStorage#listFiles(ServiceTemplate, String, boolean)
   */
  @Nullable
  FileInfo[] listFiles(@NonNull String dir, boolean deep) throws IOException;

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
  default FileInfo[] listFiles(@NonNull String dir) throws IOException {
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
   * @see TemplateStorage#deployDirectoryAsync(Path, ServiceTemplate, Predicate)
   */
  @NonNull
  Task<Boolean> deployAsync(@NonNull Path directory, @Nullable Predicate<Path> fileFilter);

  /**
   * @see TemplateStorage#deployDirectoryAsync(Path, ServiceTemplate)
   */
  @NonNull
  default Task<Boolean> deployAsync(@NonNull Path directory) {
    return this.deployAsync(directory, null);
  }

  /**
   * @see TemplateStorage#deployAsync(InputStream, ServiceTemplate)
   */
  @NonNull
  Task<Boolean> deployAsync(@NonNull InputStream inputStream);

  /**
   * @see TemplateStorage#copyAsync(ServiceTemplate, Path)
   */
  @NonNull
  Task<Boolean> copyAsync(@NonNull Path directory);

  /**
   * @see TemplateStorage#asZipInputStreamAsync(ServiceTemplate)
   */
  @NonNull
  Task<ZipInputStream> asZipInputStreamAsync();

  /**
   * @see TemplateStorage#zipTemplateAsync(ServiceTemplate)
   */
  @NonNull
  Task<InputStream> zipTemplateAsync();

  /**
   * @see TemplateStorage#deleteAsync(ServiceTemplate)
   */
  @NonNull
  Task<Boolean> deleteAsync();

  /**
   * @see TemplateStorage#createAsync(ServiceTemplate)
   */
  @NonNull
  Task<Boolean> createAsync();

  /**
   * @see TemplateStorage#hasAsync(ServiceTemplate)
   */
  @NonNull
  Task<Boolean> existsAsync();

  /**
   * @see TemplateStorage#appendOutputStreamAsync(ServiceTemplate, String)
   */
  @NonNull
  Task<OutputStream> appendOutputStreamAsync(@NonNull String path);

  /**
   * @see TemplateStorage#newOutputStreamAsync(ServiceTemplate, String)
   */
  @NonNull
  Task<OutputStream> newOutputStreamAsync(@NonNull String path);

  /**
   * @see TemplateStorage#createFileAsync(ServiceTemplate, String)
   */
  @NonNull
  Task<Boolean> createFileAsync(@NonNull String path);

  /**
   * @see TemplateStorage#createDirectoryAsync(ServiceTemplate, String)
   */
  @NonNull
  Task<Boolean> createDirectoryAsync(@NonNull String path);

  /**
   * @see TemplateStorage#hasFileAsync(ServiceTemplate, String)
   */
  @NonNull
  Task<Boolean> hasFileAsync(@NonNull String path);

  /**
   * @see TemplateStorage#deleteFileAsync(ServiceTemplate, String)
   */
  @NonNull
  Task<Boolean> deleteFileAsync(@NonNull String path);

  /**
   * @see TemplateStorage#newInputStreamAsync(ServiceTemplate, String)
   */
  @NonNull
  Task<InputStream> newInputStreamAsync(@NonNull String path);

  /**
   * @see TemplateStorage#fileInfoAsync(ServiceTemplate, String)
   */
  @NonNull
  Task<FileInfo> fileInfoAsync(@NonNull String path);

  /**
   * @see TemplateStorage#listFilesAsync(ServiceTemplate, String, boolean)
   */
  @NonNull
  Task<FileInfo[]> listFilesAsync(@NonNull String dir, boolean deep);

  /**
   * @see TemplateStorage#listFilesAsync(ServiceTemplate, boolean)
   */
  @NonNull
  default Task<FileInfo[]> listFilesAsync(boolean deep) {
    return this.listFilesAsync("", deep);
  }

  /**
   * @see TemplateStorage#listFilesAsync(ServiceTemplate, String)
   */
  @NonNull
  default Task<FileInfo[]> listFilesAsync(@NonNull String dir) {
    return this.listFilesAsync(dir, true);
  }

  /**
   * @see TemplateStorage#listFilesAsync(ServiceTemplate)
   */
  @NonNull
  default Task<FileInfo[]> listFilesAsync() {
    return this.listFilesAsync(true);
  }

}
