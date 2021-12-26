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

package de.dytanic.cloudnet.driver.template.defaults;

import de.dytanic.cloudnet.common.concurrent.Task;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class DefaultSpecificTemplateStorage implements SpecificTemplateStorage {

  private final ServiceTemplate template;
  private final TemplateStorage storage;

  private DefaultSpecificTemplateStorage(@NonNull ServiceTemplate template, @NonNull TemplateStorage storage) {
    this.template = template;
    this.storage = storage;
  }

  public static DefaultSpecificTemplateStorage of(@NonNull ServiceTemplate template, @NonNull TemplateStorage storage) {
    if (!storage.name().equals(template.storageName())) {
      throw new IllegalArgumentException(String.format(
        "Storage '%s' doesn't match the storage of the template ('%s')",
        storage.name(),
        template.storageName()));
    }
    return new DefaultSpecificTemplateStorage(template, storage);
  }

  public static DefaultSpecificTemplateStorage of(@NonNull ServiceTemplate template) {
    var storage = CloudNetDriver.instance().templateStorage(template.storageName());
    if (storage == null) {
      throw new IllegalArgumentException(String.format("Storage '%s' not found", template.storageName()));
    }
    return new DefaultSpecificTemplateStorage(template, storage);
  }

  @Override
  public @NonNull String name() {
    return this.storage.name();
  }

  @Override
  public @NonNull ServiceTemplate targetTemplate() {
    return this.template;
  }

  @Override
  public @NonNull TemplateStorage wrappedStorage() {
    return this.storage;
  }

  @Override
  public boolean deploy(@NonNull Path directory, @Nullable Predicate<Path> fileFilter) {
    return this.storage.deployDirectory(directory, this.template, fileFilter);
  }

  @Override
  public boolean deploy(@NonNull InputStream inputStream) {
    return this.storage.deploy(inputStream, this.template);
  }

  @Override
  public boolean copy(@NonNull Path directory) {
    return this.storage.copy(this.template, directory);
  }

  @Override
  public @Nullable ZipInputStream asZipInputStream() throws IOException {
    return this.storage.asZipInputStream(this.template);
  }

  @Override
  public @Nullable InputStream zipTemplate() throws IOException {
    return this.storage.zipTemplate(this.template);
  }

  @Override
  public boolean delete() {
    return this.storage.delete(this.template);
  }

  @Override
  public boolean create() {
    return this.storage.create(this.template);
  }

  @Override
  public boolean exists() {
    return this.storage.has(this.template);
  }

  @Override
  public @Nullable OutputStream appendOutputStream(@NonNull String path) throws IOException {
    return this.storage.appendOutputStream(this.template, path);
  }

  @Override
  public @Nullable OutputStream newOutputStream(@NonNull String path) throws IOException {
    return this.storage.newOutputStream(this.template, path);
  }

  @Override
  public boolean createFile(@NonNull String path) throws IOException {
    return this.storage.createFile(this.template, path);
  }

  @Override
  public boolean createDirectory(@NonNull String path) throws IOException {
    return this.storage.createDirectory(this.template, path);
  }

  @Override
  public boolean hasFile(@NonNull String path) throws IOException {
    return this.storage.hasFile(this.template, path);
  }

  @Override
  public boolean deleteFile(@NonNull String path) throws IOException {
    return this.storage.deleteFile(this.template, path);
  }

  @Override
  public @Nullable InputStream newInputStream(@NonNull String path) throws IOException {
    return this.storage.newInputStream(this.template, path);
  }

  @Override
  public @Nullable FileInfo fileInfo(@NonNull String path) throws IOException {
    return this.storage.fileInfo(this.template, path);
  }

  @Override
  public FileInfo[] listFiles(@NonNull String dir, boolean deep) throws IOException {
    return this.storage.listFiles(this.template, dir, deep);
  }

  @Override
  public FileInfo[] listFiles(boolean deep) throws IOException {
    return this.storage.listFiles(this.template, "", deep);
  }

  @Override
  public @NonNull Task<Boolean> deployAsync(@NonNull Path directory, @Nullable Predicate<Path> fileFilter) {
    return this.storage.deployDirectoryAsync(directory, this.template, fileFilter);
  }

  @Override
  public @NonNull Task<Boolean> deployAsync(@NonNull InputStream inputStream) {
    return this.storage.deployAsync(inputStream, this.template);
  }

  @Override
  public @NonNull Task<Boolean> copyAsync(@NonNull Path directory) {
    return this.storage.deployDirectoryAsync(directory, this.template);
  }

  @Override
  public @NonNull Task<ZipInputStream> asZipInputStreamAsync() {
    return this.storage.asZipInputStreamAsync(this.template);
  }

  @Override
  public @NonNull Task<InputStream> zipTemplateAsync() {
    return this.storage.zipTemplateAsync(this.template);
  }

  @Override
  public @NonNull Task<Boolean> deleteAsync() {
    return this.storage.deleteAsync(this.template);
  }

  @Override
  public @NonNull Task<Boolean> createAsync() {
    return this.storage.createAsync(this.template);
  }

  @Override
  public @NonNull Task<Boolean> existsAsync() {
    return this.storage.hasAsync(this.template);
  }

  @Override
  public @NonNull Task<OutputStream> appendOutputStreamAsync(@NonNull String path) {
    return this.storage.appendOutputStreamAsync(this.template, path);
  }

  @Override
  public @NonNull Task<OutputStream> newOutputStreamAsync(@NonNull String path) {
    return this.storage.newOutputStreamAsync(this.template, path);
  }

  @Override
  public @NonNull Task<Boolean> createFileAsync(@NonNull String path) {
    return this.storage.createFileAsync(this.template, path);
  }

  @Override
  public @NonNull Task<Boolean> createDirectoryAsync(@NonNull String path) {
    return this.storage.createDirectoryAsync(this.template, path);
  }

  @Override
  public @NonNull Task<Boolean> hasFileAsync(@NonNull String path) {
    return this.storage.hasFileAsync(this.template, path);
  }

  @Override
  public @NonNull Task<Boolean> deleteFileAsync(@NonNull String path) {
    return this.storage.deleteFileAsync(this.template, path);
  }

  @Override
  public @NonNull Task<InputStream> newInputStreamAsync(@NonNull String path) {
    return this.storage.newInputStreamAsync(this.template, path);
  }

  @Override
  public @NonNull Task<FileInfo> fileInfoAsync(@NonNull String path) {
    return this.storage.fileInfoAsync(this.template, path);
  }

  @Override
  public @NonNull Task<FileInfo[]> listFilesAsync(@NonNull String dir, boolean deep) {
    return this.storage.listFilesAsync(this.template, dir, deep);
  }

  @Override
  public @NonNull Task<FileInfo[]> listFilesAsync(boolean deep) {
    return this.storage.listFilesAsync(this.template, "", deep);
  }
}
