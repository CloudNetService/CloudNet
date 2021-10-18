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

package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalTemplateStorage implements TemplateStorage {

  public static final String LOCAL_TEMPLATE_STORAGE = "local";
  protected static final Logger LOGGER = LogManager.getLogger(LocalTemplateStorage.class);

  private final Path storageDirectory;

  public LocalTemplateStorage(@NotNull Path storageDirectory) {
    this.storageDirectory = storageDirectory;
    FileUtils.createDirectory(storageDirectory);
  }

  @Override
  public @NotNull String getName() {
    return LOCAL_TEMPLATE_STORAGE;
  }

  @Override
  public boolean deployDirectory(
    @NotNull Path directory,
    @NotNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter
  ) {
    if (Files.exists(directory)) {
      FileUtils.copyDirectory(
        directory,
        this.getTemplatePath(target),
        fileFilter == null ? null : fileFilter::test);
      return true;
    }
    return false;
  }

  @Override
  public boolean deploy(@NotNull InputStream inputStream, @NotNull ServiceTemplate target) {
    FileUtils.extractZipStream(new ZipInputStream(inputStream), this.getTemplatePath(target));
    return true;
  }

  @Override
  public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
    FileUtils.copyDirectory(this.getTemplatePath(template), directory);
    return true;
  }

  @Override
  public @Nullable InputStream zipTemplate(@NotNull ServiceTemplate template) throws IOException {
    if (this.has(template)) {
      // create a new temp file
      Path temp = FileUtils.createTempFile();
      Path zippedFile = FileUtils.zipToFile(this.getTemplatePath(template), temp);
      // open a stream to the file if possible
      if (zippedFile != null) {
        return Files.newInputStream(zippedFile, StandardOpenOption.DELETE_ON_CLOSE, LinkOption.NOFOLLOW_LINKS);
      }
    }
    return null;
  }

  @Override
  public boolean delete(@NotNull ServiceTemplate template) {
    Path templateDir = this.getTemplatePath(template);
    if (Files.notExists(templateDir)) {
      return false;
    } else {
      FileUtils.delete(templateDir);
      return true;
    }
  }

  @Override
  public boolean create(@NotNull ServiceTemplate template) {
    Path templateDir = this.getTemplatePath(template);
    if (Files.notExists(templateDir)) {
      FileUtils.createDirectory(templateDir);
      return true;
    }

    return false;
  }

  @Override
  public boolean has(@NotNull ServiceTemplate template) {
    return Files.exists(this.getTemplatePath(template));
  }

  @Override
  public @Nullable OutputStream appendOutputStream(
    @NotNull ServiceTemplate template,
    @NotNull String path
  ) throws IOException {
    Path filePath = this.getTemplatePath(template).resolve(path);
    if (Files.notExists(filePath)) {
      Files.createDirectories(filePath.getParent());
    }

    return Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  @Override
  public @Nullable OutputStream newOutputStream(
    @NotNull ServiceTemplate template,
    @NotNull String path
  ) throws IOException {
    Path filePath = this.getTemplatePath(template).resolve(path);
    if (Files.notExists(filePath)) {
      Files.createDirectories(filePath.getParent());
    }

    return Files.newOutputStream(filePath);
  }

  @Override
  public boolean createFile(
    @NotNull ServiceTemplate template,
    @NotNull String path
  ) throws IOException {
    Path filePath = this.getTemplatePath(template).resolve(path);
    if (Files.exists(filePath)) {
      return false;
    } else {
      Files.createDirectories(filePath.getParent());
      Files.createFile(filePath);
      return true;
    }
  }

  @Override
  public boolean createDirectory(
    @NotNull ServiceTemplate template,
    @NotNull String path
  ) throws IOException {
    Path dirPath = this.getTemplatePath(template).resolve(path);
    if (Files.exists(dirPath)) {
      return false;
    } else {
      Files.createDirectories(dirPath);
      return true;
    }
  }

  @Override
  public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) {
    return Files.exists(this.getTemplatePath(template).resolve(path));
  }

  @Override
  public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    Path filePath = this.getTemplatePath(template).resolve(path);
    if (Files.exists(filePath)) {
      Files.delete(filePath);
      return true;
    }
    return false;
  }

  @Override
  public @Nullable InputStream newInputStream(
    @NotNull ServiceTemplate template,
    @NotNull String path
  ) throws IOException {
    Path filePath = this.getTemplatePath(template).resolve(path);
    return Files.notExists(filePath) || Files.isDirectory(filePath) ? null : Files.newInputStream(filePath);
  }

  @Override
  public @Nullable FileInfo getFileInfo(
    @NotNull ServiceTemplate template,
    @NotNull String path
  ) throws IOException {
    Path filePath = this.getTemplatePath(template).resolve(path);
    return Files.exists(filePath) ? FileInfo.of(filePath, Paths.get(path)) : null;
  }

  @Override
  public @Nullable FileInfo[] listFiles(
    @NotNull ServiceTemplate template,
    @NotNull String dir,
    boolean deep
  ) {
    List<FileInfo> out = new ArrayList<>();
    Path root = this.getTemplatePath(template).resolve(dir);
    // walk over all files
    FileUtils.walkFileTree(root, (parent, file) -> {
      try {
        out.add(FileInfo.of(file, root.relativize(file)));
      } catch (IOException ignored) {
      }
    }, deep, $ -> true);
    // collect to an array
    return out.toArray(new FileInfo[0]);
  }

  @Override
  public @NotNull Collection<ServiceTemplate> getTemplates() {
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
          Path relative = this.storageDirectory.relativize(path);
          return new ServiceTemplate(
            relative.getName(0).toString(),
            relative.getName(1).toString(),
            LOCAL_TEMPLATE_STORAGE);
        })
        .collect(Collectors.toSet());
    } catch (IOException exception) {
      LOGGER.severe("Unable to collect templates in local template storage", exception);
      return Collections.emptyList();
    }
  }

  @Override
  public void close() {
  }

  protected @NotNull Path getTemplatePath(@NotNull ServiceTemplate template) {
    return this.storageDirectory.resolve(template.getPrefix()).resolve(template.getName());
  }
}
