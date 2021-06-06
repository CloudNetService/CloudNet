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

import de.dytanic.cloudnet.common.concurrent.ITask;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultSpecificTemplateStorage implements SpecificTemplateStorage {

  private final ServiceTemplate template;
  private final TemplateStorage storage;

  private DefaultSpecificTemplateStorage(@NotNull ServiceTemplate template, @NotNull TemplateStorage storage) {
    this.template = template;
    this.storage = storage;
  }

  public static DefaultSpecificTemplateStorage of(@NotNull ServiceTemplate template, @NotNull TemplateStorage storage) {
    if (!storage.getName().equals(template.getStorage())) {
      throw new IllegalArgumentException(String
        .format("Storage '%s' doesn't match the storage of the template ('%s')", storage.getName(),
          template.getStorage()));
    }
    return new DefaultSpecificTemplateStorage(template, storage);
  }

  public static DefaultSpecificTemplateStorage of(@NotNull ServiceTemplate template) {
    TemplateStorage storage = CloudNetDriver.getInstance().getTemplateStorage(template.getStorage());
    if (storage == null) {
      throw new IllegalArgumentException(String.format("Storage '%s' not found", template.getStorage()));
    }
    return new DefaultSpecificTemplateStorage(template, storage);
  }

  @Override
  public String getName() {
    return this.storage.getName();
  }

  @Override
  public @NotNull ServiceTemplate getTargetTemplate() {
    return this.template;
  }

  @Override
  public @NotNull TemplateStorage getWrappedStorage() {
    return this.storage;
  }

  @Override
  public boolean deploy(@NotNull Path directory, @Nullable Predicate<Path> fileFilter) {
    return this.storage.deploy(directory, this.template, fileFilter);
  }

  @Override
  public boolean deploy(@NotNull InputStream inputStream) {
    return this.storage.deploy(inputStream, this.template);
  }

  @Override
  public boolean copy(@NotNull Path directory) {
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
  public @Nullable OutputStream appendOutputStream(@NotNull String path) throws IOException {
    return this.storage.appendOutputStream(this.template, path);
  }

  @Override
  public @Nullable OutputStream newOutputStream(@NotNull String path) throws IOException {
    return this.storage.newOutputStream(this.template, path);
  }

  @Override
  public boolean createFile(@NotNull String path) throws IOException {
    return this.storage.createFile(this.template, path);
  }

  @Override
  public boolean createDirectory(@NotNull String path) throws IOException {
    return this.storage.createDirectory(this.template, path);
  }

  @Override
  public boolean hasFile(@NotNull String path) throws IOException {
    return this.storage.hasFile(this.template, path);
  }

  @Override
  public boolean deleteFile(@NotNull String path) throws IOException {
    return this.storage.deleteFile(this.template, path);
  }

  @Override
  public @Nullable InputStream newInputStream(@NotNull String path) throws IOException {
    return this.storage.newInputStream(this.template, path);
  }

  @Override
  public @Nullable FileInfo getFileInfo(@NotNull String path) throws IOException {
    return this.storage.getFileInfo(this.template, path);
  }

  @Override
  public FileInfo[] listFiles(@NotNull String dir, boolean deep) throws IOException {
    return this.storage.listFiles(this.template, dir, deep);
  }

  @Override
  public FileInfo[] listFiles(boolean deep) throws IOException {
    return this.storage.listFiles(this.template, deep);
  }

  @Override
  public @NotNull ITask<Boolean> deployAsync(@NotNull Path directory, @Nullable Predicate<Path> fileFilter) {
    return this.storage.deployAsync(directory, this.template, fileFilter);
  }

  @Override
  public @NotNull ITask<Boolean> deployAsync(@NotNull InputStream inputStream) {
    return this.storage.deployAsync(inputStream, this.template);
  }

  @Override
  public @NotNull ITask<Boolean> copyAsync(@NotNull Path directory) {
    return this.storage.deployAsync(directory, this.template);
  }

  @Override
  public @NotNull ITask<ZipInputStream> asZipInputStreamAsync() {
    return this.storage.asZipInputStreamAsync(this.template);
  }

  @Override
  public @NotNull ITask<InputStream> zipTemplateAsync() {
    return this.storage.zipTemplateAsync(this.template);
  }

  @Override
  public @NotNull ITask<Boolean> deleteAsync() {
    return this.storage.deleteAsync(this.template);
  }

  @Override
  public @NotNull ITask<Boolean> createAsync() {
    return this.storage.createAsync(this.template);
  }

  @Override
  public @NotNull ITask<Boolean> existsAsync() {
    return this.storage.hasAsync(this.template);
  }

  @Override
  public @NotNull ITask<OutputStream> appendOutputStreamAsync(@NotNull String path) {
    return this.storage.appendOutputStreamAsync(this.template, path);
  }

  @Override
  public @NotNull ITask<OutputStream> newOutputStreamAsync(@NotNull String path) {
    return this.storage.newOutputStreamAsync(this.template, path);
  }

  @Override
  public @NotNull ITask<Boolean> createFileAsync(@NotNull String path) {
    return this.storage.createFileAsync(this.template, path);
  }

  @Override
  public @NotNull ITask<Boolean> createDirectoryAsync(@NotNull String path) {
    return this.storage.createDirectoryAsync(this.template, path);
  }

  @Override
  public @NotNull ITask<Boolean> hasFileAsync(@NotNull String path) {
    return this.storage.hasFileAsync(this.template, path);
  }

  @Override
  public @NotNull ITask<Boolean> deleteFileAsync(@NotNull String path) {
    return this.storage.deleteFileAsync(this.template, path);
  }

  @Override
  public @NotNull ITask<InputStream> newInputStreamAsync(@NotNull String path) {
    return this.storage.newInputStreamAsync(this.template, path);
  }

  @Override
  public @NotNull ITask<FileInfo> getFileInfoAsync(@NotNull String path) {
    return this.storage.getFileInfoAsync(this.template, path);
  }

  @Override
  public @NotNull ITask<FileInfo[]> listFilesAsync(@NotNull String dir, boolean deep) {
    return this.storage.listFilesAsync(this.template, dir, deep);
  }

  @Override
  public @NotNull ITask<FileInfo[]> listFilesAsync(boolean deep) {
    return this.storage.listFilesAsync(this.template, deep);
  }
}
