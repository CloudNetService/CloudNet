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

package de.dytanic.cloudnet.ext.storage.ftp.storage;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPCredentials;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPType;
import de.dytanic.cloudnet.ext.storage.ftp.client.SFTPClient;
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
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SFTPTemplateStorage extends AbstractFTPStorage {

  private final SFTPClient ftpClient;

  public SFTPTemplateStorage(String name, FTPCredentials credentials) {
    super(name, credentials, FTPType.SFTP);

    this.ftpClient = new SFTPClient();
  }

  @Override
  public boolean connect() {
    return this.connect(super.credentials.getAddress().getHost(), super.credentials.getAddress().getPort(),
      super.credentials.getUsername(), super.credentials.getPassword());
  }

  private boolean connect(String host, int port, String username, String password) {
    if (this.ftpClient.isConnected()) {
      this.ftpClient.close();
    }

    return this.ftpClient.connect(host, port, username, password);
  }

  @Override
  public boolean isAvailable() {
    return this.ftpClient.isConnected();
  }

  @Override
  public void close() {
    if (this.ftpClient != null) {
      this.ftpClient.close();
    }
  }

  @Override
  public boolean deploy(@NotNull Path directory, @NotNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter) {
    return this.ftpClient.uploadDirectory(directory, this.getPath(target), fileFilter);
  }

  @Override
  public boolean deploy(@NotNull InputStream inputStream, @NotNull ServiceTemplate target) {
    return this.ftpClient.uploadDirectory(new ZipInputStream(inputStream), this.getPath(target));
  }

  @Override
  public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
    FileUtils.createDirectoryReported(directory);
    return this.ftpClient.downloadDirectory(this.getPath(template), directory.toString());
  }

  @Override
  @Nullable
  public InputStream zipTemplate(@NotNull ServiceTemplate template) throws IOException {
    if (!this.has(template)) {
      return null;
    }

    Path tempFile = FileUtils.createTempFile();
    try (OutputStream stream = Files.newOutputStream(tempFile)) {
      this.ftpClient.zipDirectory(this.getPath(template), stream);
      return Files.newInputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE, LinkOption.NOFOLLOW_LINKS);
    }
  }

  @Override
  public boolean delete(@NotNull ServiceTemplate template) {
    return this.ftpClient.deleteDirectory(this.getPath(template));
  }

  @Override
  public boolean create(@NotNull ServiceTemplate template) {
    if (this.has(template)) {
      return false;
    }
    this.ftpClient.createDirectories(this.getPath(template));
    return true;
  }

  @Override
  public boolean has(@NotNull ServiceTemplate template) {
    return this.ftpClient.existsDirectory(this.getPath(template));
  }

  @Nullable
  @Override
  public OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.ftpClient.appendOutputStream(this.getPath(template) + "/" + path);
  }

  @Nullable
  @Override
  public OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.ftpClient.openOutputStream(this.getPath(template) + "/" + path);
  }

  @Override
  public void completeDataTransfer() {
  }

  @Override
  public boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.ftpClient.createFile(this.getPath(template) + "/" + path);
  }

  @Override
  public boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) {
    this.ftpClient.createDirectories(this.getPath(template) + "/" + path);
    return true;
  }

  @Override
  public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.ftpClient.existsFile(this.getPath(template) + "/" + path);
  }

  @Override
  public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) {
    String fullPath = this.getPath(template) + "/" + path;
    SftpATTRS attrs = this.ftpClient.getAttrs(fullPath);
    return attrs != null && attrs.isDir() ? this.ftpClient.deleteDirectory(fullPath)
      : this.ftpClient.deleteFile(fullPath);
  }

  @Override
  public @Nullable InputStream newInputStream(@NotNull ServiceTemplate template, @NotNull String path) {
    String fullPath = this.getPath(template) + "/" + path;
    SftpATTRS attrs = this.ftpClient.getAttrs(fullPath);
    return attrs != null && !attrs.isDir() ? this.ftpClient.loadFile(fullPath) : null;
  }

  @Override
  public @Nullable FileInfo getFileInfo(@NotNull ServiceTemplate template, @NotNull String path) {
    SftpATTRS attrs = this.ftpClient.getAttrs(this.getPath(template) + "/" + path);
    if (attrs == null) {
      return null;
    }

    String filename = path;

    int index = path.lastIndexOf('/');
    if (index != -1 && index < path.length() - 1) {
      filename = path.substring(index + 1);
    }

    return this.asInfo(path, filename, attrs);
  }

  private String removeLeadingSlash(String input) {
    if (input.startsWith("/")) {
      input = input.substring(1);
    }
    if (input.endsWith("/")) {
      input = input.substring(0, input.length() - 1);
    }
    return input;
  }

  @Override
  public FileInfo[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir, boolean deep) {
    Collection<FileInfo> files = new ArrayList<>();
    dir = this.removeLeadingSlash(dir);

    if (!this.listFiles(this.getPath(template) + "/" + dir, dir, files, deep)) {
      return null;
    }
    return files.toArray(new FileInfo[0]);
  }

  private boolean listFiles(String directory, String rawRelativeDirectory, Collection<FileInfo> files, boolean deep) {
    String relativeDirectory = this.removeLeadingSlash(rawRelativeDirectory);

    if (!this.ftpClient.existsDirectory(directory)) {
      return false;
    }

    Collection<ChannelSftp.LsEntry> entries = this.ftpClient.listFiles(directory);
    if (entries == null) {
      return false;
    }

    for (ChannelSftp.LsEntry entry : entries) {
      String relativePath =
        relativeDirectory.isEmpty() ? entry.getFilename() : relativeDirectory + "/" + entry.getFilename();

      files.add(this.asInfo(relativePath, entry.getFilename(), entry.getAttrs()));

      if (deep && entry.getAttrs().isDir()) {
        this.listFiles(directory + "/" + entry.getFilename(), relativePath, files, true);
      }
    }

    return true;
  }

  private FileInfo asInfo(String path, String name, SftpATTRS attrs) {
    return new FileInfo(
      path, name,
      attrs.isDir(), false,
      -1, 1000 * (long) attrs.getMTime(), 1000 * (long) attrs.getATime(),
      attrs.getSize()
    );
  }

  @Override
  public @NotNull Collection<ServiceTemplate> getTemplates() {
    Collection<ChannelSftp.LsEntry> entries = this.ftpClient.listFiles(super.baseDirectory);
    if (entries == null) {
      return Collections.emptyList();
    }

    Collection<ServiceTemplate> templates = new ArrayList<>(entries.size());
    for (ChannelSftp.LsEntry entry : entries) {
      String prefix = entry.getFilename();

      Collection<ChannelSftp.LsEntry> prefixEntries = this.ftpClient.listFiles(super.baseDirectory + "/" + prefix);
      if (prefixEntries != null) {
        for (ChannelSftp.LsEntry nameEntry : prefixEntries) {
          String name = nameEntry.getFilename();

          templates.add(new ServiceTemplate(prefix, name, this.getName()));
        }
      }
    }

    return templates;
  }

  private String getPath(ServiceTemplate template) {
    return this.baseDirectory + "/" + template.getPrefix() + "/" + template.getName();
  }

}
