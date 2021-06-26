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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TemplateStorage extends AutoCloseable, INameable {

  /**
   * Deploys the following directory files to the target template storage. If the given file filter is not null, method
   * will apply every single file to the given file filter and only deploy files that were allowed by the filter.
   *
   * @param directory  the directory to be deployed
   * @param target     the template to be deployed to
   * @param fileFilter the filter for files that should be deployed
   * @return whether it was copied successfully
   */
  boolean deploy(@NotNull Path directory, @NotNull ServiceTemplate target, @Nullable Predicate<Path> fileFilter);

  /**
   * Copies the given directory into the given template.
   *
   * @param directory the directory to be deployed
   * @param target    the template to be deployed to
   * @return whether it was copied successfully
   */
  default boolean deploy(@NotNull Path directory, @NotNull ServiceTemplate target) {
    return this.deploy(directory, target, null);
  }

  /**
   * Copies the given stream into the given template.
   *
   * @param inputStream the zipped data to be deployed
   * @param target      the template to be deployed to
   * @return whether it was copied successfully
   */
  boolean deploy(@NotNull InputStream inputStream, @NotNull ServiceTemplate target);

  /**
   * Copies the given template into the given directory
   *
   * @param template  the template to copy the files from
   * @param directory the target directory to copy the files to
   * @return whether it was copied successfully
   */
  boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory);

  /**
   * Gets a template into a {@link ZipInputStream}.
   *
   * @param template the template to be zipped
   * @return a new {@link ZipInputStream} or null if it the template doesn't exist
   * @throws IOException if an I/O error occurred
   */
  @Nullable
  default ZipInputStream asZipInputStream(@NotNull ServiceTemplate template) throws IOException {
    InputStream inputStream = this.zipTemplate(template);
    return inputStream == null ? null : new ZipInputStream(inputStream);
  }

  /**
   * Zips a template into an {@link InputStream}.
   *
   * @param template the template to be zipped
   * @return a new {@link InputStream} or null if it the template doesn't exist
   * @throws IOException if an I/O error occurred
   */
  @Nullable
  InputStream zipTemplate(@NotNull ServiceTemplate template) throws IOException;

  /**
   * Deletes the given template if it exists.
   *
   * @param template the template to be deleted
   * @return whether the template was successfully deleted, {@code false} if it doesn't exists
   */
  boolean delete(@NotNull ServiceTemplate template);

  /**
   * Creates the given template if it doesn't exists.
   *
   * @param template the template to be deleted
   * @return whether the template was successfully created, {@code false} if it already exists
   */
  boolean create(@NotNull ServiceTemplate template);

  /**
   * Checks whether the given template exists.
   *
   * @param template the template to be checked
   * @return whether the template exists or not
   */
  boolean has(@NotNull ServiceTemplate template);

  /**
   * Creates a new {@link OutputStream} that will append its content to the file at the given path. If the file at the
   * given path doesn't exist, it will be created. To finish your modifications, you'll need to close the {@link
   * OutputStream}.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return a new {@link OutputStream} or {@code null} if it couldn't be opened
   * @throws IOException if an I/O error occurred
   */
  @Nullable
  OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

  /**
   * Creates a new {@link OutputStream} that will override its content to the file at the given path. If the file at the
   * given path doesn't exist, it will be created. To finish your modifications, you'll need to close the {@link
   * OutputStream}.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return a new {@link OutputStream} or {@code null} if it couldn't be opened
   * @throws IOException if an I/O error occurred
   */
  @Nullable
  OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

  /**
   * Creates a new file in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file was created successfully, {@code false} if it already exists
   * @throws IOException if an I/O error occurred
   */
  boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

  /**
   * Creates a new directory in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the directory relative to the template
   * @return whether the directory was created successfully, {@code false} if it already exists
   * @throws IOException if an I/O error occurred
   */
  boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

  /**
   * Checks whether the given path/directory exists in the given template, it doesn't matter if it is a directory or a
   * file.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file exists
   * @throws IOException if an I/O error occurred
   */
  boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

  /**
   * Deletes a file in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file was deleted successfully, {@code false} if it already exists
   * @throws IOException if an I/O error occurred
   */
  boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

  /**
   * Creates a new {@link InputStream} with the data of the file at the given path in the given template.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return the new {@link InputStream} or {@code null} if the file doesn't exist/is a directory
   * @throws IOException if an I/O error occurred
   */
  @Nullable
  InputStream newInputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

  /**
   * Retrieves information about a specified file/directory.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return the {@link FileInfo} or {@code null} if the file/directory doesn't exist
   * @throws IOException if an I/O error occurred
   */
  @Nullable
  FileInfo getFileInfo(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

  /**
   * Lists all files in the given directory.
   *
   * @param template the template where the target file is located at
   * @param dir      the directory
   * @param deep     whether the contents of sub directories should also be discovered
   * @return a list of all files in the given directory or {@code null} if the given directory doesn't exist/is not a
   * directory
   * @throws IOException if an I/O error occurred
   */
  @Nullable
  FileInfo[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir, boolean deep) throws IOException;

  /**
   * Lists all files in the root directory of the template.
   *
   * @param template the template where the target file is located at
   * @param deep     whether the contents of sub directories should also be discovered
   * @return a list of all files in the given directory or {@code null} if the given directory doesn't exist/is not a
   * directory
   * @throws IOException if an I/O error occurred
   */
  @Nullable
  default FileInfo[] listFiles(@NotNull ServiceTemplate template, boolean deep) throws IOException {
    return this.listFiles(template, "", deep);
  }

  /**
   * Lists all files in the given directory (including sub directories).
   *
   * @param template the template where the target file is located at
   * @param dir      the directory
   * @return a list of all files in the given directory or {@code null} if the given directory doesn't exist/is not a
   * directory
   * @throws IOException if an I/O error occurred
   */
  @Nullable
  default FileInfo[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir) throws IOException {
    return this.listFiles(template, dir, true);
  }

  /**
   * Lists all files in the root directory of the template (including sub directories).
   *
   * @param template the template where the target file is located at
   * @return a list of all files in the given directory or {@code null} if the given directory doesn't exist/is not a
   * directory
   * @throws IOException if an I/O error occurred
   */
  @Nullable
  default FileInfo[] listFiles(@NotNull ServiceTemplate template) throws IOException {
    return this.listFiles(template, true);
  }

  /**
   * Gets a list of all templates that exist in this storage. Modifications to the collection won't have any effect.
   *
   * @return a list of all templates
   */
  @NotNull
  Collection<ServiceTemplate> getTemplates();

  /**
   * Closes this storage, after it has been closed, no more interaction to this storage should be done and might lead to
   * errors.
   *
   * @throws IOException if an I/O error occurred
   */
  @Override
  void close() throws IOException;

  /**
   * Deploys the following directory files to the target template storage. If the given file filter is not null, method
   * will apply every single file to the given file filter and only deploy files that were allowed by the filter.
   *
   * @param directory  the directory to be deployed
   * @param target     the template to be deployed to
   * @param fileFilter the filter for files that should be deployed
   * @return whether it was copied successfully
   */
  @NotNull
  ITask<Boolean> deployAsync(@NotNull Path directory, @NotNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter);

  /**
   * Copies the given directory into the given template.
   *
   * @param directory the directory to be deployed
   * @param target    the template to be deployed to
   * @return whether it was copied successfully
   */
  @NotNull
  default ITask<Boolean> deployAsync(@NotNull Path directory, @NotNull ServiceTemplate target) {
    return this.deployAsync(directory, target, null);
  }

  /**
   * Copies the given directory into the given template.
   *
   * @param inputStream the zipped data to be deployed
   * @param target      the template to be deployed to
   * @return whether it was copied successfully
   */
  @NotNull
  ITask<Boolean> deployAsync(@NotNull InputStream inputStream, @NotNull ServiceTemplate target);

  /**
   * Copies the given template into the given directory
   *
   * @param template  the template to copy the files from
   * @param directory the target directory to copy the files to
   * @return whether it was copied successfully
   */
  @NotNull
  ITask<Boolean> copyAsync(@NotNull ServiceTemplate template, @NotNull Path directory);

  /**
   * Gets a template into a {@link ZipInputStream}.
   *
   * @param template the template to be zipped
   * @return a new {@link ZipInputStream} or null if it the template doesn't exist
   */
  @NotNull
  default ITask<ZipInputStream> asZipInputStreamAsync(@NotNull ServiceTemplate template) {
    return this.zipTemplateAsync(template)
      .map(inputStream -> inputStream == null ? null : new ZipInputStream(inputStream));
  }

  /**
   * Zips a template into an {@link InputStream}.
   *
   * @param template the template to be zipped
   * @return a new {@link InputStream} or null if it the template doesn't exist
   */
  @NotNull
  ITask<InputStream> zipTemplateAsync(@NotNull ServiceTemplate template);

  /**
   * Deletes the given template if it exists.
   *
   * @param template the template to be deleted
   * @return whether the template was successfully deleted, {@code false} if it doesn't exists
   */
  @NotNull
  ITask<Boolean> deleteAsync(@NotNull ServiceTemplate template);

  /**
   * Creates the given template if it doesn't exists.
   *
   * @param template the template to be deleted
   * @return whether the template was successfully created, {@code false} if it already exists
   */
  @NotNull
  ITask<Boolean> createAsync(@NotNull ServiceTemplate template);

  /**
   * Checks whether the given template exists.
   *
   * @param template the template to be checked
   * @return whether the template exists or not
   */
  @NotNull
  ITask<Boolean> hasAsync(@NotNull ServiceTemplate template);

  /**
   * Creates a new {@link OutputStream} that will append its content to the file at the given path. If the file at the
   * given path doesn't exist, it will be created. To finish your modifications, you'll need to close the {@link
   * OutputStream}.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return a new {@link OutputStream} or {@code null} if it couldn't be opened
   */
  @NotNull
  ITask<OutputStream> appendOutputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path);

  /**
   * Creates a new {@link OutputStream} that will override its content to the file at the given path. If the file at the
   * given path doesn't exist, it will be created. To finish your modifications, you'll need to close the {@link
   * OutputStream}.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return a new {@link OutputStream} or {@code null} if it couldn't be opened
   */
  @NotNull
  ITask<OutputStream> newOutputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path);

  /**
   * Creates a new file in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file was created successfully, {@code false} if it already exists
   */
  @NotNull
  ITask<Boolean> createFileAsync(@NotNull ServiceTemplate template, @NotNull String path);

  /**
   * Creates a new directory in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the directory relative to the template
   * @return whether the directory was created successfully, {@code false} if it already exists
   */
  @NotNull
  ITask<Boolean> createDirectoryAsync(@NotNull ServiceTemplate template, @NotNull String path);

  /**
   * Checks whether the given path/directory exists in the given template, it doesn't matter if it is a directory or a
   * file.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file exists
   */
  @NotNull
  ITask<Boolean> hasFileAsync(@NotNull ServiceTemplate template, @NotNull String path);

  /**
   * Deletes a file in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file was deleted successfully, {@code false} if it already exists
   */
  @NotNull
  ITask<Boolean> deleteFileAsync(@NotNull ServiceTemplate template, @NotNull String path);

  /**
   * Creates a new {@link InputStream} with the data of the file at the given path in the given template.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return the new {@link InputStream} or {@code null} if the file doesn't exist/is a directory
   */
  @NotNull
  ITask<InputStream> newInputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path);

  /**
   * Retrieves information about a specified file/directory.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return the {@link FileInfo} or {@code null} if the file/directory doesn't exist
   */
  @NotNull
  ITask<FileInfo> getFileInfoAsync(@NotNull ServiceTemplate template, @NotNull String path);

  /**
   * Lists all files in the given directory.
   *
   * @param template the template where the target file is located at
   * @param dir      the directory
   * @param deep     whether the contents of sub directories should also be discovered
   * @return a list of all files in the given directory or {@code null} if the given directory doesn't exist/is not a
   * directory
   */
  @NotNull
  ITask<FileInfo[]> listFilesAsync(@NotNull ServiceTemplate template, @NotNull String dir, boolean deep);

  /**
   * Lists all files in the root directory of the template.
   *
   * @param template the template where the target file is located at
   * @param deep     whether the contents of sub directories should also be discovered
   * @return a list of all files in the given directory or {@code null} if the given directory doesn't exist/is not a
   * directory
   */
  @NotNull
  default ITask<FileInfo[]> listFilesAsync(@NotNull ServiceTemplate template, boolean deep) {
    return this.listFilesAsync(template, "", deep);
  }

  /**
   * Lists all files in the given directory (including sub directories).
   *
   * @param template the template where the target file is located at
   * @param dir      the directory
   * @return a list of all files in the given directory or {@code null} if the given directory doesn't exist/is not a
   * directory
   */
  @NotNull
  default ITask<FileInfo[]> listFilesAsync(@NotNull ServiceTemplate template, @NotNull String dir) {
    return this.listFilesAsync(template, dir, true);
  }

  /**
   * Lists all files in the root directory of the template (including sub directories).
   *
   * @param template the template where the target file is located at
   * @return a list of all files in the given directory or {@code null} if the given directory doesn't exist/is not a
   * directory
   */
  @NotNull
  default ITask<FileInfo[]> listFilesAsync(@NotNull ServiceTemplate template) {
    return this.listFilesAsync(template, true);
  }

  /**
   * Gets a list of all templates that exist in this storage. Modifications to the collection won't have any effect.
   *
   * @return a list of all templates
   */
  @NotNull
  ITask<Collection<ServiceTemplate>> getTemplatesAsync();

  /**
   * Closes this storage, after it has been closed, no more interaction to this storage should be done and might lead to
   * errors.
   */
  @NotNull
  ITask<Void> closeAsync();

}
