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

package eu.cloudnetservice.node.impl.template;

import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.FileInfo;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
import eu.cloudnetservice.utils.base.io.ZipUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class LocalTemplateStorage implements TemplateStorage {

  public static final String LOCAL_TEMPLATE_STORAGE = "local";

  private final Path storageDirectory;

  public LocalTemplateStorage(@NonNull Path storageDirectory) {
    this.storageDirectory = storageDirectory;
    FileUtil.createDirectory(storageDirectory);
  }

  @Override
  public @NonNull String name() {
    return LOCAL_TEMPLATE_STORAGE;
  }

  @Override
  public boolean deployDirectory(
    @NonNull ServiceTemplate target,
    @NonNull Path directory,
    @Nullable Predicate<Path> filter
  ) {
    if (Files.exists(directory)) {
      FileUtil.copyDirectory(
        directory,
        this.getTemplatePath(target),
        filter == null ? null : filter::test);
      return true;
    }
    return false;
  }

  @Override
  public boolean deploy(@NonNull ServiceTemplate target, @NonNull InputStream inputStream) {
    ZipUtil.extractZipStream(new ZipInputStream(inputStream), this.getTemplatePath(target));
    return true;
  }

  @Override
  public boolean pull(@NonNull ServiceTemplate template, @NonNull Path directory) {
    FileUtil.copyDirectory(this.getTemplatePath(template), directory);
    return true;
  }

  @Override
  public @Nullable InputStream zipTemplate(@NonNull ServiceTemplate template) throws IOException {
    if (this.contains(template)) {
      // create a new temp file
      var temp = FileUtil.createTempFile();
      var zippedFile = ZipUtil.zipToFile(this.getTemplatePath(template), temp);
      // open a stream to the file if possible
      if (zippedFile != null) {
        return Files.newInputStream(zippedFile, StandardOpenOption.DELETE_ON_CLOSE, LinkOption.NOFOLLOW_LINKS);
      }
    }
    return null;
  }

  @Override
  public boolean delete(@NonNull ServiceTemplate template) {
    var templateDir = this.getTemplatePath(template);
    if (Files.notExists(templateDir)) {
      return false;
    } else {
      FileUtil.delete(templateDir);
      return true;
    }
  }

  @Override
  public boolean create(@NonNull ServiceTemplate template) {
    var templateDir = this.getTemplatePath(template);
    if (Files.notExists(templateDir)) {
      FileUtil.createDirectory(templateDir);
      return true;
    }

    return false;
  }

  @Override
  public boolean contains(@NonNull ServiceTemplate template) {
    return Files.exists(this.getTemplatePath(template));
  }

  @Override
  public @Nullable OutputStream appendOutputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    var filePath = this.getTemplatePath(template).resolve(path);
    if (Files.notExists(filePath)) {
      Files.createDirectories(filePath.getParent());
    }

    return Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  @Override
  public @Nullable OutputStream newOutputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    var filePath = this.getTemplatePath(template).resolve(path);
    if (Files.notExists(filePath)) {
      Files.createDirectories(filePath.getParent());
    }

    return Files.newOutputStream(filePath);
  }

  @Override
  public boolean createFile(@NonNull ServiceTemplate template, @NonNull String path) {
    var filePath = this.getTemplatePath(template).resolve(path);
    if (Files.exists(filePath)) {
      return false;
    } else {
      try {
        Files.createDirectories(filePath.getParent());
        Files.createFile(filePath);
        return true;
      } catch (IOException exception) {
        return false;
      }
    }
  }

  @Override
  public boolean createDirectory(@NonNull ServiceTemplate template, @NonNull String path) {
    var dirPath = this.getTemplatePath(template).resolve(path);
    if (Files.exists(dirPath)) {
      return false;
    } else {
      FileUtil.createDirectory(dirPath);
      return true;
    }
  }

  @Override
  public boolean hasFile(@NonNull ServiceTemplate template, @NonNull String path) {
    return Files.exists(this.getTemplatePath(template).resolve(path));
  }

  @Override
  public boolean deleteFile(@NonNull ServiceTemplate template, @NonNull String path) {
    var filePath = this.getTemplatePath(template).resolve(path);
    if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
      FileUtil.delete(filePath);
      return true;
    }

    return false;
  }

  @Override
  public @Nullable InputStream newInputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    var filePath = this.getTemplatePath(template).resolve(path);
    return Files.notExists(filePath) || Files.isDirectory(filePath) ? null : Files.newInputStream(filePath);
  }

  @Override
  public @Nullable FileInfo fileInfo(@NonNull ServiceTemplate template, @NonNull String path) {
    try {
      var filePath = this.getTemplatePath(template).resolve(path);
      return Files.exists(filePath) ? FileInfo.of(filePath, Path.of(path)) : null;
    } catch (IOException exception) {
      return null;
    }
  }

  @Override
  public @NonNull Collection<FileInfo> listFiles(
    @NonNull ServiceTemplate template,
    @NonNull String dir,
    boolean deep
  ) {
    List<FileInfo> out = new ArrayList<>();
    var root = this.getTemplatePath(template);
    // walk over all files
    FileUtil.walkFileTree(root.resolve(dir), (parent, file) -> {
      try {
        out.add(FileInfo.of(file, root.relativize(file)));
      } catch (IOException ignored) {
      }
    }, deep, $ -> true);
    // collect to an array
    return out;
  }

  @Override
  public @NonNull Collection<ServiceTemplate> templates() {
    try {
      return Files.list(this.storageDirectory)
        .filter(Files::isDirectory)
        .flatMap(directory -> {
          try {
            return Files.list(directory);
          } catch (IOException exception) {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .filter(Files::isDirectory)
        .map(path -> {
          var relative = this.storageDirectory.relativize(path);
          return ServiceTemplate.builder()
            .storage(this.name())
            .prefix(relative.getName(0).toString())
            .name(relative.getName(1).toString())
            .build();
        })
        .collect(Collectors.toSet());
    } catch (IOException exception) {
      return Collections.emptyList();
    }
  }

  @Override
  public void close() {
  }

  @Override
  public @NonNull CompletableFuture<Boolean> deployDirectoryAsync(
    @NonNull ServiceTemplate target,
    @NonNull Path directory,
    @Nullable Predicate<Path> filter
  ) {
    return TaskUtil.supplyAsync(() -> this.deployDirectory(target, directory, filter));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> deployAsync(
    @NonNull ServiceTemplate target,
    @NonNull InputStream inputStream
  ) {
    return TaskUtil.supplyAsync(() -> this.deploy(target, inputStream));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> pullAsync(@NonNull ServiceTemplate template, @NonNull Path directory) {
    return TaskUtil.supplyAsync(() -> this.pull(template, directory));
  }

  @Override
  public @NonNull CompletableFuture<InputStream> zipTemplateAsync(@NonNull ServiceTemplate template) {
    return TaskUtil.supplyAsync(() -> this.zipTemplate(template));
  }

  @Override
  public @NonNull CompletableFuture<ZipInputStream> openZipInputStreamAsync(@NonNull ServiceTemplate template) {
    return TaskUtil.supplyAsync(() -> this.openZipInputStream(template));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> deleteAsync(@NonNull ServiceTemplate template) {
    return TaskUtil.supplyAsync(() -> this.delete(template));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> createAsync(@NonNull ServiceTemplate template) {
    return TaskUtil.supplyAsync(() -> this.create(template));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> containsAsync(@NonNull ServiceTemplate template) {
    return TaskUtil.supplyAsync(() -> this.contains(template));
  }

  @Override
  public @NonNull CompletableFuture<OutputStream> appendOutputStreamAsync(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) {
    return TaskUtil.supplyAsync(() -> this.appendOutputStream(template, path));
  }

  @Override
  public @NonNull CompletableFuture<OutputStream> newOutputStreamAsync(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) {
    return TaskUtil.supplyAsync(() -> this.newOutputStream(template, path));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> createFileAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return TaskUtil.supplyAsync(() -> this.createFile(template, path));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> createDirectoryAsync(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) {
    return TaskUtil.supplyAsync(() -> this.createDirectory(template, path));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> hasFileAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return TaskUtil.supplyAsync(() -> this.hasFile(template, path));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> deleteFileAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return TaskUtil.supplyAsync(() -> this.deleteFile(template, path));
  }

  @Override
  public @NonNull CompletableFuture<InputStream> newInputStreamAsync(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) {
    return TaskUtil.supplyAsync(() -> this.newInputStream(template, path));
  }

  @Override
  public @NonNull CompletableFuture<FileInfo> fileInfoAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return TaskUtil.supplyAsync(() -> this.fileInfo(template, path));
  }

  @Override
  public @NonNull CompletableFuture<Collection<FileInfo>> listFilesAsync(
    @NonNull ServiceTemplate template,
    @NonNull String dir,
    boolean deep
  ) {
    return TaskUtil.supplyAsync(() -> this.listFiles(template, dir, deep));
  }

  @Override
  public @NonNull CompletableFuture<Collection<ServiceTemplate>> templatesAsync() {
    return TaskUtil.supplyAsync(this::templates);
  }

  @Override
  public @NonNull CompletableFuture<Void> closeAsync() {
    return CompletableFuture.completedFuture(null);
  }

  protected @NonNull Path getTemplatePath(@NonNull ServiceTemplate template) {
    return this.storageDirectory.resolve(template.prefix()).resolve(template.name());
  }
}
