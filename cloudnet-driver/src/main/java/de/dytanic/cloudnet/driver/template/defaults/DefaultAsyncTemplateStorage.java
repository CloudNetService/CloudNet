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
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultAsyncTemplateStorage implements TemplateStorage {

  @Override
  public boolean deploy(@NotNull Path directory, @NotNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter) {
    return this.deployAsync(directory, target, fileFilter).get(20, TimeUnit.SECONDS, false);
  }

  @Override
  public boolean deploy(@NotNull Path directory, @NotNull ServiceTemplate target) {
    return this.deployAsync(directory, target).get(20, TimeUnit.SECONDS, false);
  }

  @Override
  public boolean deploy(@NotNull InputStream inputStream, @NotNull ServiceTemplate target) {
    return this.deployAsync(inputStream, target).get(20, TimeUnit.SECONDS, false);
  }

  @Override
  public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
    return this.copyAsync(template, directory).get(20, TimeUnit.SECONDS, false);
  }

  @Override
  public @Nullable ZipInputStream asZipInputStream(@NotNull ServiceTemplate template) throws IOException {
    return this.catchIOException(this.asZipInputStreamAsync(template), null);
  }

  @Override
  public @Nullable InputStream zipTemplate(@NotNull ServiceTemplate template) throws IOException {
    return this.catchIOException(this.zipTemplateAsync(template), null);
  }

  @Override
  public boolean delete(@NotNull ServiceTemplate template) {
    return this.deleteAsync(template).get(5, TimeUnit.SECONDS, false);
  }

  @Override
  public boolean create(@NotNull ServiceTemplate template) {
    return this.createAsync(template).get(5, TimeUnit.SECONDS, false);
  }

  @Override
  public boolean has(@NotNull ServiceTemplate template) {
    return this.hasAsync(template).get(5, TimeUnit.SECONDS, false);
  }

  @Override
  public @Nullable OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path)
    throws IOException {
    return this.catchIOException(this.appendOutputStreamAsync(template, path), null);
  }

  @Override
  public @Nullable OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path)
    throws IOException {
    return this.catchIOException(this.newOutputStreamAsync(template, path), null);
  }

  @Override
  public boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return this.catchIOException(this.createFileAsync(template, path), false);
  }

  @Override
  public boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return this.catchIOException(this.createDirectoryAsync(template, path), false);
  }

  @Override
  public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return this.catchIOException(this.hasFileAsync(template, path), false);
  }

  @Override
  public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return this.catchIOException(this.deleteFileAsync(template, path), false);
  }

  @Override
  public @Nullable InputStream newInputStream(@NotNull ServiceTemplate template, @NotNull String path)
    throws IOException {
    return this.catchIOException(this.newInputStreamAsync(template, path), null);
  }

  @Override
  public @Nullable FileInfo getFileInfo(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return this.catchIOException(this.getFileInfoAsync(template, path), null);
  }

  @Override
  public FileInfo[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir, boolean deep) throws IOException {
    return this.catchIOException(this.listFilesAsync(template, dir, deep), null);
  }

  @Override
  public FileInfo[] listFiles(@NotNull ServiceTemplate template, boolean deep) throws IOException {
    return this.catchIOException(this.listFilesAsync(template, deep), null);
  }

  @Override
  public @NotNull Collection<ServiceTemplate> getTemplates() {
    return this.getTemplatesAsync().get(5, TimeUnit.SECONDS, Collections.emptyList());
  }

  @Override
  public void close() throws IOException {
    this.catchIOException(this.closeAsync(), null);
  }

  private <V> V catchIOException(ITask<V> task, V def) throws IOException {
    try {
      return task.get(20, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException exception) {
      exception.printStackTrace();
      return def;
    } catch (ExecutionException exception) {
      if (exception.getCause() instanceof IOException) {
        throw (IOException) exception.getCause();
      }
    }
    return def;
  }
}
