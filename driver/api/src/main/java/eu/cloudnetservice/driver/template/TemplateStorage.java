/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.template;

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides an interface to execute all types of operations on templates stored on any file system. A template storage
 * is bound to that file system and can not interoperate with different file systems.
 * <p>
 * All operations which are executable from a template storage are based on the assumption that you either can access
 * files using an output/input stream or via the local file system. Once a template storage was closed via the provided
 * close method it is not safe anymore to access data through it. These operations might succeed as expected but will
 * most likely fail. It is safe to access a template storage from multiple threads concurrently, but there are no checks
 * made if the template storage is still in use when the storage is getting closed, meaning that other threads which are
 * still accessing the storage might die due to an exception.
 *
 * @since 4.0
 */
public interface TemplateStorage extends AutoCloseable, Named {

  /**
   * Deploys all files in the given directory to the associated target in this template storage for the given template.
   *
   * @param target    the template to deploy the directory to.
   * @param directory the directory to deploy to the storage.
   * @return true if the operation completed successfully, false otherwise.
   * @throws NullPointerException if the given directory or service template is null.
   */
  default boolean deployDirectory(@NonNull ServiceTemplate target, @NonNull Path directory) {
    return this.deployDirectory(target, directory, null);
  }

  /**
   * Deploys all files in the given directory to the associated target in this template storage for the given template.
   * All files in the directory will be passed to the given filter, if present. If the filter returns false for a path
   * the file or directory will not be deployed into the storage.
   *
   * @param directory the directory to deploy to the storage.
   * @param target    the template to deploy the directory to.
   * @param filter    an optional filter which can be used to filter out files which shouldn't get deployed.
   * @return true if the operation completed successfully, false otherwise.
   * @throws NullPointerException if the given directory or service template is null.
   */
  boolean deployDirectory(@NonNull ServiceTemplate target, @NonNull Path directory, @Nullable Predicate<Path> filter);

  /**
   * Deploys all files in the given input stream to the given target template. The given input stream is expected to be
   * backed by a zip file, each entry of that zip file will be deployed. If anything else than a stream of a zip file is
   * given to this method it will either not deploy anything or fail with an exception.
   *
   * @param target      the template to deploy the data inside the zip file to.
   * @param inputStream the stream to the zip file which should be deployed.
   * @return true if the operation completed successfully, false otherwise.
   * @throws NullPointerException if the given input stream or target template is null.
   */
  boolean deploy(@NonNull ServiceTemplate target, @NonNull InputStream inputStream);

  /**
   * Pulls the template data which is stored in this template storage to the given directory. If any file stored in the
   * template in this storage is already present in the target directory it will be overridden.
   *
   * @param template  the template to pull the files of.
   * @param directory the target directory to pull the files into.
   * @return true if the operation completed successfully, false otherwise.
   * @throws NullPointerException if the given template or target directory is null.
   */
  boolean pull(@NonNull ServiceTemplate template, @NonNull Path directory);

  /**
   * Pulls the data of the given template into a temporary directory and zip it. The returned input stream is a stream
   * which can read from the created zip file but is not necessarily a zip input stream. If you specifically want a zip
   * input stream consider using {@link #openZipInputStream(ServiceTemplate)} instead.
   * <p>
   * When the returned input stream is closed the created temporary zip file will be deleted automatically.
   *
   * @param template the template to download and zip.
   * @return a stream which reads from the pulled and zipped template file, null if the template doesn't exist.
   * @throws IOException          if an I/O error occurred while zipping the template data.
   * @throws NullPointerException if the given template is null.
   */
  @Nullable
  InputStream zipTemplate(@NonNull ServiceTemplate template) throws IOException;

  /**
   * Pulls the data of the given template into a temporary directory and zip it. When the returned input stream is
   * closed the created temporary zip file will be deleted automatically.
   *
   * @param template the template to download and zip.
   * @return a stream which reads from the pulled and zipped template file, null if the template doesn't exist.
   * @throws IOException          if an I/O error occurred while zipping the template data.
   * @throws NullPointerException if the given template is null.
   */
  default @Nullable ZipInputStream openZipInputStream(@NonNull ServiceTemplate template) throws IOException {
    var inputStream = this.zipTemplate(template);
    return inputStream == null ? null : new ZipInputStream(inputStream);
  }

  /**
   * Deletes the given template completely from this template storage.
   *
   * @param template the template to delete.
   * @return true if the given template was deleted successfully, false otherwise.
   * @throws NullPointerException if the given template is null.
   */
  boolean delete(@NonNull ServiceTemplate template);

  /**
   * Creates the given template in this template storage if it doesn't exist already.
   *
   * @param template the template to create.
   * @return true if the template was created successfully, false otherwise.
   * @throws NullPointerException if the given template is null.
   */
  boolean create(@NonNull ServiceTemplate template);

  /**
   * Checks if this template storage contains any data associated with the given template.
   *
   * @param template the template to check for.
   * @return true if this storage contains the given template, false otherwise.
   * @throws NullPointerException if the given template is null.
   */
  boolean contains(@NonNull ServiceTemplate template);

  /**
   * Creates a new output stream which appends to the file at the given path in the given template in this storage. If
   * the file at the given path doesn't exist already it will be created automatically. After all the content was
   * written to the given output stream it <strong>must</strong> be closed in order to persist your changes in this
   * storage.
   *
   * @param template the template in which the file to append to is located.
   * @param path     the path to the file in the given template to open the append-stream for.
   * @return an output stream which appends to the given file in the given template.
   * @throws IOException          if an I/O error occurred.
   * @throws NullPointerException if the given template or file path is null.
   */
  @Nullable
  OutputStream appendOutputStream(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Creates a new output stream which overrides the content of the file at the given path in the given template in this
   * storage. If the file at the given path doesn't exist already it will be created automatically. After all the
   * content was written to the given output stream it <strong>must</strong> be closed in order to persist your changes
   * in this storage.
   *
   * @param template the template in which the file to write to is located.
   * @param path     the path to the file in the given template to open the stream for.
   * @return an output stream which overrides to the given file in the given template.
   * @throws IOException          if an I/O error occurred.
   * @throws NullPointerException if the given template or file path is null.
   */
  @Nullable
  OutputStream newOutputStream(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Creates a new, empty file at the given path in the given template in this storage if the file didn't exist
   * already.
   *
   * @param template the template to create the file in.
   * @param path     the path to the file in the given template.
   * @return true if the file was created successfully, false otherwise.
   * @throws NullPointerException if the given template or path is null.
   */
  boolean createFile(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Creates a new, empty directory at the given path in the given template in this storage if the directory didn't
   * exist already.
   *
   * @param template the template to create the directory in.
   * @param path     the path to the directory in the given template.
   * @return true if the directory was created successfully, false otherwise.
   * @throws NullPointerException if the given template or path is null.
   */
  boolean createDirectory(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Checks if a file or directory exists at the given path in the given template in this template storage.
   *
   * @param template the template to check for the file existence in.
   * @param path     the path to the file in the template to check for.
   * @return true if a file or directory exists at the given path, false otherwise.
   * @throws NullPointerException if the given template or path is null.
   */
  boolean hasFile(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Deletes the given file at the given path in the given template in this storage. This method is only able to delete
   * files, directories cannot be deleted using this method.
   *
   * @param template the template in which the file to delete is located in.
   * @param path     the path to the file in the template to delete.
   * @return true if the file at the given path was deleted successfully, false otherwise.
   * @throws NullPointerException if the given template or path is null.
   */
  boolean deleteFile(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Opens a new input stream to read the content of the file at the given path in the given template in this storage.
   * This method returns null if either the file doesn't exist or is a directory.
   *
   * @param template the template in which the file to read is located in.
   * @param path     the path to the file in the template to read.
   * @return a new input stream to read the content of the file at the given path, null if the file isn't readable.
   * @throws IOException          if an I/O error occurred.
   * @throws NullPointerException if the given template or template path is null.
   */
  @Nullable
  InputStream newInputStream(@NonNull ServiceTemplate template, @NonNull String path) throws IOException;

  /**
   * Retrieves information about the specified file or directory at the given path in the given template in this
   * storage.
   *
   * @param template the template in which the file or directory to get the info of is located in.
   * @param path     the path to the file or directory to get the info of.
   * @return an information about the file or directory at the given path.
   * @throws NullPointerException if the given template or path is null.
   */
  @Nullable
  FileInfo fileInfo(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Lists all files in the given directory and computes a file information of them. Optionally all files in
   * subdirectories are listed as well if the deep option is set to true.
   *
   * @param template the template in which the directory to list the files of is located.
   * @param dir      the directory to list the files and optionally the directories in.
   * @param deep     whether the content of subdirectories in the given directory should be included.
   * @return all files which are located in the given directory.
   * @throws NullPointerException if the given template or directory is null.
   */
  @NonNull
  Collection<FileInfo> listFiles(@NonNull ServiceTemplate template, @NonNull String dir, boolean deep);

  /**
   * Gets a list of all templates that exist in this storage. Modifications to the collection won't have any effect.
   * <p>
   * The templates in the returned collection will not have the following options configured correctly as they are not
   * stored in the template storage directly:
   * <ol>
   *   <li>priority
   *   <li>alwaysCopyToStaticServices
   * </ol>
   *
   * @return all templates which are located in this storage.
   */
  @NonNull
  Collection<ServiceTemplate> templates();

  /**
   * Closes this storage and releases all resources which are associated with it (if any). Calls which are made to this
   * storage after it has been closed might either not have any effect or will result in an exception.
   *
   * @throws IOException if an I/O error occurred while closing the underlying resources of this storage.
   */
  @Override
  void close() throws IOException;

  /**
   * Deploys all files in the given directory to the associated target in this template storage for the given template.
   *
   * @param target    the template to deploy the directory to.
   * @param directory the directory to deploy to the storage.
   * @return a task completed with true if the operation completed successfully, false otherwise.
   * @throws NullPointerException if the given directory or service template is null.
   */
  default @NonNull CompletableFuture<Boolean> deployDirectoryAsync(
    @NonNull ServiceTemplate target,
    @NonNull Path directory
  ) {
    return this.deployDirectoryAsync(target, directory, null);
  }

  /**
   * Deploys all files in the given directory to the associated target in this template storage for the given template.
   * All files in the directory will be passed to the given filter, if present. If the filter returns false for a path
   * the file or directory will not be deployed into the storage.
   *
   * @param directory the directory to deploy to the storage.
   * @param target    the template to deploy the directory to.
   * @param filter    an optional filter which can be used to filter out files which shouldn't get deployed.
   * @return a task completed with true if the operation completed successfully, false otherwise.
   * @throws NullPointerException if the given directory or service template is null.
   */
  @NonNull
  CompletableFuture<Boolean> deployDirectoryAsync(
    @NonNull ServiceTemplate target,
    @NonNull Path directory,
    @Nullable Predicate<Path> filter);

  /**
   * Deploys all files in the given input stream to the given target template. The given input stream is expected to be
   * backed by a zip file, each entry of that zip file will be deployed. If anything else than a stream of a zip file is
   * given to this method it will either not deploy anything or fail with an exception.
   *
   * @param target      the template to deploy the data inside the zip file to.
   * @param inputStream the stream to the zip file which should be deployed.
   * @return a task completed with true if the operation completed successfully, false otherwise.
   * @throws NullPointerException if the given input stream or target template is null.
   */
  @NonNull
  CompletableFuture<Boolean> deployAsync(@NonNull ServiceTemplate target, @NonNull InputStream inputStream);

  /**
   * Pulls the template data which is stored in this template storage to the given directory. If any file stored in the
   * template in this storage is already present in the target directory it will be overridden.
   *
   * @param template  the template to pull the files of.
   * @param directory the target directory to pull the files into.
   * @return a task completed with true if the operation completed successfully, false otherwise.
   * @throws NullPointerException if the given template or target directory is null.
   */
  @NonNull
  CompletableFuture<Boolean> pullAsync(@NonNull ServiceTemplate template, @NonNull Path directory);

  /**
   * Pulls the data of the given template into a temporary directory and zip it. The returned input stream is a stream
   * which can read from the created zip file but is not necessarily a zip input stream. If you specifically want a zip
   * input stream consider using {@link #openZipInputStreamAsync(ServiceTemplate)} instead.
   * <p>
   * When the returned input stream is closed the created temporary zip file will be deleted automatically.
   *
   * @param template the template to download and zip.
   * @return a task completed with a stream which reads from the pulled and zipped template file.
   * @throws NullPointerException if the given template is null.
   */
  @NonNull
  CompletableFuture<InputStream> zipTemplateAsync(@NonNull ServiceTemplate template);

  /**
   * Pulls the data of the given template into a temporary directory and zip it. When the returned input stream is
   * closed the created temporary zip file will be deleted automatically.
   *
   * @param template the template to download and zip.
   * @return a task completed with a stream which reads from the pulled and zipped template file.
   * @throws NullPointerException if the given template is null.
   */
  @NonNull
  CompletableFuture<ZipInputStream> openZipInputStreamAsync(@NonNull ServiceTemplate template);

  /**
   * Deletes the given template completely from this template storage.
   *
   * @param template the template to delete.
   * @return a task completed with true if the given template was deleted successfully, false otherwise.
   * @throws NullPointerException if the given template is null.
   */
  @NonNull
  CompletableFuture<Boolean> deleteAsync(@NonNull ServiceTemplate template);

  /**
   * Creates the given template in this template storage if it doesn't exist already.
   *
   * @param template the template to create.
   * @return a task completed with true if the template was created successfully, false otherwise.
   * @throws NullPointerException if the given template is null.
   */
  @NonNull
  CompletableFuture<Boolean> createAsync(@NonNull ServiceTemplate template);

  /**
   * Checks if this template storage contains any data associated with the given template.
   *
   * @param template the template to check for.
   * @return a task completed true if this storage contains the given template, false otherwise.
   * @throws NullPointerException if the given template is null.
   */
  @NonNull
  CompletableFuture<Boolean> containsAsync(@NonNull ServiceTemplate template);

  /**
   * Creates a new output stream which appends to the file at the given path in the given template in this storage. If
   * the file at the given path doesn't exist already it will be created automatically. After all the content was
   * written to the given output stream it <strong>must</strong> be closed in order to persist your changes in this
   * storage.
   *
   * @param template the template in which the file to append to is located.
   * @param path     the path to the file in the given template to open the append-stream for.
   * @return a task completed with an output stream which appends to the given file in the given template.
   * @throws NullPointerException if the given template or file path is null.
   */
  @NonNull
  CompletableFuture<OutputStream> appendOutputStreamAsync(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Creates a new output stream which overrides the content of the file at the given path in the given template in this
   * storage. If the file at the given path doesn't exist already it will be created automatically. After all the
   * content was written to the given output stream it <strong>must</strong> be closed in order to persist your changes
   * in this storage.
   *
   * @param template the template in which the file to write to is located.
   * @param path     the path to the file in the given template to open the stream for.
   * @return a task completed with an output stream which overrides to the given file in the given template.
   * @throws NullPointerException if the given template or file path is null.
   */
  @NonNull
  CompletableFuture<OutputStream> newOutputStreamAsync(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Creates a new, empty file at the given path in the given template in this storage if the file didn't exist
   * already.
   *
   * @param template the template to create the file in.
   * @param path     the path to the file in the given template.
   * @return a task completed with true if the file was created successfully, false otherwise.
   * @throws NullPointerException if the given template or path is null.
   */
  @NonNull
  CompletableFuture<Boolean> createFileAsync(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Creates a new, empty directory at the given path in the given template in this storage if the directory didn't
   * exist already.
   *
   * @param template the template to create the directory in.
   * @param path     the path to the directory in the given template.
   * @return a task completed with true if the directory was created successfully, false otherwise.
   * @throws NullPointerException if the given template or path is null.
   */
  @NonNull
  CompletableFuture<Boolean> createDirectoryAsync(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Checks if a file or directory exists at the given path in the given template in this template storage.
   *
   * @param template the template to check for the file existence in.
   * @param path     the path to the file in the template to check for.
   * @return a task completed with true if a file or directory exists at the given path, false otherwise.
   * @throws NullPointerException if the given template or path is null.
   */
  @NonNull
  CompletableFuture<Boolean> hasFileAsync(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Deletes the given file at the given path in the given template in this storage. This method is only able to delete
   * files, directories cannot be deleted using this method.
   *
   * @param template the template in which the file to delete is located in.
   * @param path     the path to the file in the template to delete.
   * @return a task completed with true if the file at the given path was deleted successfully, false otherwise.
   * @throws NullPointerException if the given template or path is null.
   */
  @NonNull
  CompletableFuture<Boolean> deleteFileAsync(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Opens a new input stream to read the content of the file at the given path in the given template in this storage.
   * This method returns null if either the file doesn't exist or is a directory.
   *
   * @param template the template in which the file to read is located in.
   * @param path     the path to the file in the template to read.
   * @return a task completed with a new input stream to read the content of the file at the given path.
   * @throws NullPointerException if the given template or template path is null.
   */
  @NonNull
  CompletableFuture<InputStream> newInputStreamAsync(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Retrieves information about the specified file or directory at the given path in the given template in this
   * storage.
   *
   * @param template the template in which the file or directory to get the info of is located in.
   * @param path     the path to the file or directory to get the info of.
   * @return a task completed with an information about the file or directory at the given path.
   * @throws NullPointerException if the given template or path is null.
   */
  @NonNull
  CompletableFuture<FileInfo> fileInfoAsync(@NonNull ServiceTemplate template, @NonNull String path);

  /**
   * Lists all files in the given directory and computes a file information of them. Optionally all files in
   * subdirectories are listed as well if the deep option is set to true.
   *
   * @param template the template in which the directory to list the files of is located.
   * @param dir      the directory to list the files and optionally the directories in.
   * @param deep     whether the content of subdirectories in the given directory should be included.
   * @return a task completed with all files which are located in the given directory.
   * @throws NullPointerException if the given template or directory is null.
   */
  @NonNull
  CompletableFuture<Collection<FileInfo>> listFilesAsync(
    @NonNull ServiceTemplate template,
    @NonNull String dir,
    boolean deep);

  /**
   * Gets a list of all templates that exist in this storage. Modifications to the collection won't have any effect.
   * <p>
   * The templates in the returned collection will not have the following options configured correctly as they are not
   * stored in the template storage directly:
   * <ol>
   *   <li>priority
   *   <li>alwaysCopyToStaticServices
   * </ol>
   *
   * @return a task completed with all templates which are located in this storage.
   */
  @NonNull
  CompletableFuture<Collection<ServiceTemplate>> templatesAsync();

  /**
   * Closes this storage and releases all resources which are associated with it (if any). Calls which are made to this
   * storage after it has been closed might either not have any effect or will result in an exception.
   *
   * @return a task completed when the template storage was closed.
   */
  @NonNull
  CompletableFuture<Void> closeAsync();
}
