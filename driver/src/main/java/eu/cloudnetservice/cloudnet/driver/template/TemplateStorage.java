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

import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.concurrent.CompletableTask;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCValidation;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@RPCValidation("deployDirectory.*")
public interface TemplateStorage extends AutoCloseable, Nameable {

  /**
   * Deploys the following directory files to the target template storage. If the given file filter is not null, method
   * will apply every single file to the given file filter and only deploy files that were allowed by the filter.
   *
   * @param directory  the directory to be deployed
   * @param target     the template to be deployed to
   * @param fileFilter the filter for files that should be deployed
   * @return whether it was copied successfully
   */
  boolean deployDirectory(@NonNull Path directory, @NonNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter);

  /**
   * Copies the given directory into the given template.
   *
   * @param directory the directory to be deployed
   * @param target    the template to be deployed to
   * @return whether it was copied successfully
   */
  default boolean deployDirectory(@NonNull Path directory, @NonNull ServiceTemplate target) {
    return this.deployDirectory(directory, target, null);
  }

  /**
   * Copies the given stream into the given template.
   *
   * @param inputStream the zipped data to be deployed
   * @param target      the template to be deployed to
   * @return whether it was copied successfully
   */
  boolean deploy(@NonNull InputStream inputStream, @NonNull ServiceTemplate target);

  /**
   * Copies the given template into the given directory
   *
   * @param template  the template to copy the files from
   * @param directory the target directory to copy the files to
   * @return whether it was copied successfully
   */
  boolean copy(@NonNull ServiceTemplate template, @NonNull Path directory);

  /**
   * Gets a template into a {@link ZipInputStream}.
   *
   * @param template the template to be zipped
   * @return a new {@link ZipInputStream} or null if the template doesn't exist
   * @throws IOException if an I/O error occurred
   */
  default @Nullable ZipInputStream asZipInputStream(@NonNull ServiceTemplate template) throws IOException {
    var inputStream = this.zipTemplate(template);
    return inputStream == null ? null : new ZipInputStream(inputStream);
  }

  /**
   * Zips a template into an {@link InputStream}.
   *
   * @param template the template to be zipped
   * @return a new {@link InputStream} or null if the template doesn't exist
   * @throws IOException if an I/O error occurred
   */
  @Nullable InputStream zipTemplate(@NonNull ServiceTemplate template) throws IOException;

  /**
   * Deletes the given template if it exists.
   *
   * @param template the template to be deleted
   * @return whether the template was successfully deleted, false if it doesn't exist
   */
  boolean delete(@NonNull ServiceTemplate template);

  /**
   * Creates the given template if it doesn't exists.
   *
   * @param template the template to be deleted
   * @return whether the template was successfully created, false if it already exists
   */
  boolean create(@NonNull ServiceTemplate template);

  /**
   * Checks whether the given template exists.
   *
   * @param template the template to be checked
   * @return whether the template exists or not
   */
  boolean has(@NonNull ServiceTemplate template);

  /**
   * Creates a new {@link OutputStream} that will append its content to the file at the given path. If the file at the
   * given path doesn't exist, it will be created. To finish your modifications, you'll need to close the {@link
   * OutputStream}.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return a new {@link OutputStream} or null if it couldn't be opened
   * @throws IOException if an I/O error occurred
   */
  @Nullable OutputStream appendOutputStream(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Creates a new {@link OutputStream} that will override its content to the file at the given path. If the file at the
   * given path doesn't exist, it will be created. To finish your modifications, you'll need to close the {@link
   * OutputStream}.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return a new {@link OutputStream} or null if it couldn't be opened
   * @throws IOException if an I/O error occurred
   */
  @Nullable OutputStream newOutputStream(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Creates a new file in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file was created successfully, false if it already exists
   * @throws IOException if an I/O error occurred
   */
  boolean createFile(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Creates a new directory in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the directory relative to the template
   * @return whether the directory was created successfully, false if it already exists
   * @throws IOException if an I/O error occurred
   */
  boolean createDirectory(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Checks whether the given path/directory exists in the given template, it doesn't matter if it is a directory or a
   * file.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file exists
   * @throws IOException if an I/O error occurred
   */
  boolean hasFile(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Deletes a file in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file was deleted successfully, false if it already exists
   * @throws IOException if an I/O error occurred
   */
  boolean deleteFile(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Creates a new {@link InputStream} with the data of the file at the given path in the given template.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return the new {@link InputStream} or null if the file doesn't exist/is a directory
   * @throws IOException if an I/O error occurred
   */
  @Nullable InputStream newInputStream(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Retrieves information about a specified file/directory.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return the {@link FileInfo} or null if the file/directory doesn't exist
   * @throws IOException if an I/O error occurred
   */
  @Nullable FileInfo fileInfo(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Lists all files in the given directory.
   *
   * @param template the template where the target file is located at
   * @param dir      the directory
   * @param deep     whether the contents of sub directories should also be discovered
   * @return a list of all files in the given directory or null if the given directory doesn't exist/is not a directory
   * @throws IOException if an I/O error occurred
   */
  @Nullable FileInfo[] listFiles(@NonNull ServiceTemplate template, @NonNull String dir, boolean deep)
    throws IOException;

  /**
   * Gets a list of all templates that exist in this storage. Modifications to the collection won't have any effect.
   *
   * @return a list of all templates
   */
  @NonNull Collection<ServiceTemplate> templates();

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
  default @NonNull Task<Boolean> deployDirectoryAsync(
    @NonNull Path directory,
    @NonNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter
  ) {
    return CompletableTask.supply(() -> this.deployDirectory(directory, target, fileFilter));
  }

  /**
   * Copies the given directory into the given template.
   *
   * @param directory the directory to be deployed
   * @param target    the template to be deployed to
   * @return whether it was copied successfully
   */
  default @NonNull Task<Boolean> deployDirectoryAsync(@NonNull Path directory, @NonNull ServiceTemplate target) {
    return this.deployDirectoryAsync(directory, target, null);
  }

  /**
   * Copies the given directory into the given template.
   *
   * @param inputStream the zipped data to be deployed
   * @param target      the template to be deployed to
   * @return whether it was copied successfully
   */
  default @NonNull Task<Boolean> deployAsync(@NonNull InputStream inputStream, @NonNull ServiceTemplate target) {
    return CompletableTask.supply(() -> this.deploy(inputStream, target));
  }

  /**
   * Copies the given template into the given directory
   *
   * @param template  the template to copy the files from
   * @param directory the target directory to copy the files to
   * @return whether it was copied successfully
   */
  default @NonNull Task<Boolean> copyAsync(@NonNull ServiceTemplate template, @NonNull Path directory) {
    return CompletableTask.supply(() -> this.copy(template, directory));
  }

  /**
   * Gets a template into a {@link ZipInputStream}.
   *
   * @param template the template to be zipped
   * @return a new {@link ZipInputStream} or null if it the template doesn't exist
   */
  default @NonNull Task<ZipInputStream> asZipInputStreamAsync(@NonNull ServiceTemplate template) {
    return this.zipTemplateAsync(template)
      .map(inputStream -> inputStream == null ? null : new ZipInputStream(inputStream));
  }

  /**
   * Zips a template into an {@link InputStream}.
   *
   * @param template the template to be zipped
   * @return a new {@link InputStream} or null if it the template doesn't exist
   */
  default @NonNull Task<InputStream> zipTemplateAsync(@NonNull ServiceTemplate template) {
    return CompletableTask.supply(() -> this.zipTemplate(template));
  }

  /**
   * Deletes the given template if it exists.
   *
   * @param template the template to be deleted
   * @return whether the template was successfully deleted, false if it doesn't exists
   */
  default @NonNull Task<Boolean> deleteAsync(@NonNull ServiceTemplate template) {
    return CompletableTask.supply(() -> this.delete(template));
  }

  /**
   * Creates the given template if it doesn't exists.
   *
   * @param template the template to be deleted
   * @return whether the template was successfully created, false if it already exists
   */
  default @NonNull Task<Boolean> createAsync(@NonNull ServiceTemplate template) {
    return CompletableTask.supply(() -> this.create(template));
  }

  /**
   * Checks whether the given template exists.
   *
   * @param template the template to be checked
   * @return whether the template exists or not
   */
  default @NonNull Task<Boolean> hasAsync(@NonNull ServiceTemplate template) {
    return CompletableTask.supply(() -> this.has(template));
  }

  /**
   * Creates a new {@link OutputStream} that will append its content to the file at the given path. If the file at the
   * given path doesn't exist, it will be created. To finish your modifications, you'll need to close the {@link
   * OutputStream}.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return a new {@link OutputStream} or null if it couldn't be opened
   */
  default @NonNull Task<OutputStream> appendOutputStreamAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return CompletableTask.supply(() -> this.appendOutputStream(template, path));
  }

  /**
   * Creates a new {@link OutputStream} that will override its content to the file at the given path. If the file at the
   * given path doesn't exist, it will be created. To finish your modifications, you'll need to close the {@link
   * OutputStream}.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return a new {@link OutputStream} or null if it couldn't be opened
   */
  default @NonNull Task<OutputStream> newOutputStreamAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return CompletableTask.supply(() -> this.newOutputStream(template, path));
  }

  /**
   * Creates a new file in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file was created successfully, false if it already exists
   */
  default @NonNull Task<Boolean> createFileAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return CompletableTask.supply(() -> this.createFile(template, path));
  }

  /**
   * Creates a new directory in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the directory relative to the template
   * @return whether the directory was created successfully, false if it already exists
   */
  default @NonNull Task<Boolean> createDirectoryAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return CompletableTask.supply(() -> this.createDirectory(template, path));
  }

  /**
   * Checks whether the given path/directory exists in the given template, it doesn't matter if it is a directory or a
   * file.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file exists
   */
  default @NonNull Task<Boolean> hasFileAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return CompletableTask.supply(() -> this.hasFile(template, path));
  }

  /**
   * Deletes a file in the given template at the given path if it doesn't exist.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return whether the file was deleted successfully, false if it already exists
   */
  default @NonNull Task<Boolean> deleteFileAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return CompletableTask.supply(() -> this.deleteFile(template, path));
  }

  /**
   * Creates a new {@link InputStream} with the data of the file at the given path in the given template.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return the new {@link InputStream} or null if the file doesn't exist/is a directory
   */
  default @NonNull Task<InputStream> newInputStreamAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return CompletableTask.supply(() -> this.newInputStream(template, path));
  }

  /**
   * Retrieves information about a specified file/directory.
   *
   * @param template the template where the target file is located at
   * @param path     the path to the file relative to the template
   * @return the {@link FileInfo} or null if the file/directory doesn't exist
   */
  default @NonNull Task<FileInfo> fileInfoAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return CompletableTask.supply(() -> this.fileInfo(template, path));
  }

  /**
   * Lists all files in the given directory.
   *
   * @param template the template where the target file is located at
   * @param dir      the directory
   * @param deep     whether the contents of sub directories should also be discovered
   * @return a list of all files in the given directory or null if the given directory doesn't exist/is not a directory
   */
  default @NonNull Task<FileInfo[]> listFilesAsync(
    @NonNull ServiceTemplate template,
    @NonNull String dir,
    boolean deep
  ) {
    return CompletableTask.supply(() -> this.listFiles(template, dir, deep));
  }

  /**
   * Gets a list of all templates that exist in this storage. Modifications to the collection won't have any effect.
   *
   * @return a list of all templates
   */
  default @NonNull Task<Collection<ServiceTemplate>> templatesAsync() {
    return CompletableTask.supply(this::templates);
  }

  /**
   * Closes this storage, after it has been closed, no more interaction to this storage should be done and might lead to
   * errors.
   */
  default @NonNull Task<Void> closeAsync() {
    return CompletableTask.supply(() -> {
      this.close();
      return null;
    });
  }
}
