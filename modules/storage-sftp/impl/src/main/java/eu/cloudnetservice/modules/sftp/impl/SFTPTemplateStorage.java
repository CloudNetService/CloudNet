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

package eu.cloudnetservice.modules.sftp.impl;

import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.FileInfo;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.modules.sftp.config.SFTPTemplateStorageConfig;
import eu.cloudnetservice.modules.sftp.impl.sshj.ActiveHeartbeatKeepAliveProvider;
import eu.cloudnetservice.modules.sftp.impl.sshj.FilteringLocalFileSource;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
import eu.cloudnetservice.utils.base.io.ZipUtil;
import io.vavr.CheckedFunction1;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;
import lombok.NonNull;
import net.schmizz.sshj.Config;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SSHException;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFTPTemplateStorage implements TemplateStorage {

  protected static final String REMOTE_DIR_FORMAT = "%s/%s/%s";
  protected static final Logger LOGGER = LoggerFactory.getLogger(SFTPTemplateStorage.class);

  private final Config config;
  private final SFTPClientPool pool;
  private final SFTPTemplateStorageConfig storageConfig;

  private volatile SSHClient sshClient;

  public SFTPTemplateStorage(@NonNull SFTPTemplateStorageConfig config) {
    this.storageConfig = config;
    // init the config
    this.config = new DefaultConfig();
    this.config.setKeepAliveProvider(ActiveHeartbeatKeepAliveProvider.INSTANCE);
    // init the pool
    this.pool = new SFTPClientPool(config.clientPoolSize(), () -> {
      var client = this.sshClient;
      // check if the client was ever initialized
      if (client != null) {
        // check if the client is still connected
        if (client.isConnected() && client.isAuthenticated()) {
          // we can use the current client
          return client;
        } else {
          // the current client is not available anymore
          client.disconnect();
          this.sshClient = null;
        }
      }
      // create and set a new client
      this.sshClient = new SSHClient(this.config);
      this.sshClient.setConnectTimeout(5000);
      this.sshClient.setRemoteCharset(StandardCharsets.UTF_8);
      // load the known hosts file if given
      var knownHosts = config.knownHostFile();
      if (knownHosts == null) {
        // always trust the server
        this.sshClient.addHostKeyVerifier(new PromiscuousVerifier());
      } else {
        // load the known hosts file
        this.sshClient.loadKnownHosts(knownHosts.toFile());
      }
      // connect to the server
      this.sshClient.connect(config.address().host(), config.address().port());
      // authenticate the client with the correct auth method
      if (config.sshKeyPath() != null) {
        this.sshClient.authPublickey(
          config.username(),
          this.sshClient.loadKeys(config.sshKeyPath().toString(), config.sshKeyPassword()));
      } else {
        this.sshClient.authPassword(config.username(), config.password());
      }
      // return the created client
      return this.sshClient;
    });
  }

  @Override
  public @NonNull String name() {
    return this.storageConfig.storage();
  }

  @Override
  public boolean deployDirectory(
    @NonNull ServiceTemplate target,
    @NonNull Path directory,
    @Nullable Predicate<Path> filter
  ) {
    return this.executeWithClient(client -> {
      client.put(new FilteringLocalFileSource(directory, filter), this.constructRemotePath(target));
      return true;
    }, false);
  }

  @Override
  public boolean deploy(@NonNull ServiceTemplate target, @NonNull InputStream inputStream) {
    var temp = ZipUtil.extract(inputStream, FileUtil.createTempFile());
    if (temp != null) {
      try {
        return this.deployDirectory(target, temp, null);
      } finally {
        FileUtil.delete(temp);
      }
    }
    return false;
  }

  @Override
  public boolean pull(@NonNull ServiceTemplate template, @NonNull Path directory) {
    return this.executeWithClient(client -> {
      // we cannot call "get" directly as that would cause a download of the file into a directory
      // which is called the same way the template is called
      var target = new FileSystemFile(directory.toFile());
      for (var fileInfo : client.ls(this.constructRemotePath(template))) {
        client.get(fileInfo.getPath(), target);
      }
      return true;
    }, false);
  }

  @Override
  public @Nullable InputStream zipTemplate(@NonNull ServiceTemplate template) {
    return this.executeWithClient(client -> {
      var localTarget = FileUtil.createTempFile();
      if (this.pull(template, localTarget)) {
        return ZipUtil.zipToStream(localTarget);
      } else {
        return null;
      }
    }, null);
  }

  @Override
  public boolean delete(@NonNull ServiceTemplate template) {
    return this.executeWithClient(client -> {
      var templatePath = this.constructRemotePath(template);
      var attributes = client.statExistence(templatePath);
      // checks if the remote directory actually exists
      if (attributes != null && attributes.getType() == FileMode.Type.DIRECTORY) {
        // now we are sure that the template exists and can remove it
        this.deleteDir(client, templatePath);
        client.rmdir(templatePath);
        return true;
      } else {
        return false;
      }
    }, false);
  }

  protected void deleteDir(@NonNull SFTPClient client, @NonNull String dir) throws IOException {
    for (var info : client.ls(dir)) {
      // delete the directory if recursive
      if (info.isDirectory()) {
        this.deleteDir(client, info.getPath());
        client.rmdir(info.getPath());
      } else {
        client.rm(info.getPath());
      }
    }
  }

  @Override
  public boolean create(@NonNull ServiceTemplate template) {
    return this.executeWithClient(client -> {
      client.mkdirs(this.constructRemotePath(template));
      return true;
    }, false);
  }

  @Override
  public boolean contains(@NonNull ServiceTemplate template) {
    return this.executeWithClient(client -> {
      var attr = client.statExistence(this.constructRemotePath(template));
      return attr != null && attr.getType() == FileMode.Type.DIRECTORY;
    }, false);
  }

  @Override
  public @Nullable OutputStream appendOutputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    return this.newOutputStream(template, path, OpenMode.CREAT, OpenMode.WRITE, OpenMode.APPEND);
  }

  @Override
  public @Nullable OutputStream newOutputStream(@NonNull ServiceTemplate st, @NonNull String path) throws IOException {
    return this.newOutputStream(st, path, OpenMode.CREAT, OpenMode.WRITE, OpenMode.TRUNC);
  }

  protected @Nullable OutputStream newOutputStream(
    @NonNull ServiceTemplate st,
    @NonNull String path,
    OpenMode @NonNull ... modes
  ) throws IOException {
    var client = this.pool.takeClient();
    // ensure that the parent directories of the file exist
    this.createParentDirectories(st, path, client);
    // open the file
    var file = client.open(this.constructRemotePath(st, path), EnumSet.of(modes[0], modes));
    // create a new output stream which returns the client to the pool when closing
    return file.new RemoteFileOutputStream() {
      @Override
      public void close() throws IOException {
        super.close();
        SFTPTemplateStorage.this.pool.returnClient(client);
      }
    };
  }

  @Override
  public boolean createFile(@NonNull ServiceTemplate template, @NonNull String path) {
    return this.executeWithClient(client -> {
      // ensure that the parent directories exist
      this.createParentDirectories(template, path, client);
      client.open(this.constructRemotePath(template, path), EnumSet.of(OpenMode.CREAT));
      return true;
    }, false);
  }

  @Override
  public boolean createDirectory(@NonNull ServiceTemplate template, @NonNull String path) {
    return this.executeWithClient(client -> {
      client.mkdirs(this.constructRemotePath(template, path));
      return true;
    }, false);
  }

  @Override
  public boolean hasFile(@NonNull ServiceTemplate template, @NonNull String path) {
    return this.executeWithClient(
      client -> client.statExistence(this.constructRemotePath(template, path)) != null,
      false);
  }

  @Override
  public boolean deleteFile(@NonNull ServiceTemplate template, @NonNull String path) {
    return this.executeWithClient(client -> {
      client.rm(this.constructRemotePath(template, path));
      return true;
    }, false);
  }

  @Override
  public @Nullable InputStream newInputStream(@NonNull ServiceTemplate st, @NonNull String path) throws IOException {
    var client = this.pool.takeClient();
    // open the file
    var file = client.open(this.constructRemotePath(st, path), EnumSet.of(OpenMode.CREAT, OpenMode.READ));
    // create a new input stream which returns the client to the pool when closing
    return file.new RemoteFileInputStream() {
      @Override
      public void close() throws IOException {
        super.close();
        SFTPTemplateStorage.this.pool.returnClient(client);
      }
    };
  }

  @Override
  public @Nullable FileInfo fileInfo(@NonNull ServiceTemplate template, @NonNull String path) {
    return this.executeWithClient(client -> {
      var attr = client.statExistence(this.constructRemotePath(template, path));
      return attr == null ? null : this.createFileInfo(attr, path);
    }, null);
  }

  @Override
  public @NonNull Collection<FileInfo> listFiles(
    @NonNull ServiceTemplate template,
    @NonNull String dir,
    boolean deep
  ) {
    return this.executeWithClient(client -> {
      Set<FileInfo> result = new HashSet<>();
      this.ls(client, result, template, dir, deep);
      return result;
    }, null);
  }

  protected void createParentDirectories(
    @NonNull ServiceTemplate template,
    @NonNull String path,
    @NonNull SFTPClient client
  ) throws IOException {
    var lastSeparator = path.lastIndexOf('/');
    var pathInTemplate = lastSeparator == -1 ? "" : path.substring(0, lastSeparator);

    client.mkdirs(this.constructRemotePath(template, pathInTemplate));
  }

  protected void ls(
    @NonNull SFTPClient client,
    @NonNull Set<FileInfo> result,
    @NonNull ServiceTemplate template,
    @NonNull String dir,
    boolean deep
  ) throws Exception {
    for (var info : client.ls(this.constructRemotePath(template, dir))) {
      // add the file as a result
      result.add(this.createFileInfo(info.getAttributes(), info.getPath()));
      // if the file is a directory, and we should check recursive do that
      if (info.isDirectory() && deep) {
        this.ls(client, result, template, dir.endsWith("/") ? dir : (dir + '/') + info.getName(), true);
      }
    }
  }

  @Override
  public @NonNull Collection<ServiceTemplate> templates() {
    return this.executeWithClient(client -> {
      Set<ServiceTemplate> templates = new HashSet<>();
      for (var info : client.ls(this.storageConfig.baseDirectory())) {
        if (info.isDirectory()) {
          for (var template : client.ls(this.storageConfig.baseDirectory() + '/' + info.getName())) {
            templates.add(ServiceTemplate.builder()
              .prefix(info.getName())
              .name(template.getName())
              .storage(this.storageConfig.storage())
              .build());
          }
        }
      }
      return templates;
    }, Collections.emptySet());
  }

  @Override
  public void close() throws IOException {
    // if the base-client is null there are no pooled clients as well, but we need to mark the pool itself as closed
    this.pool.close();

    // check if we've ever opened a session
    var client = this.sshClient;
    if (client != null) {
      client.disconnect();
    }
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
    return TaskUtil.runAsync(this::close);
  }

  protected @NonNull String constructRemotePath(@NonNull ServiceTemplate template, String @NonNull ... parents) {
    return String.format(
      REMOTE_DIR_FORMAT,
      this.storageConfig.baseDirectory(),
      template.fullName(),
      String.join("/", parents));
  }

  protected @NonNull FileInfo createFileInfo(@NonNull FileAttributes attributes, @NonNull String path) {
    // get the file name - bit hacky but ok
    var parts = path.split("/");
    // create the file info
    return new FileInfo(
      path,
      parts.length == 0 ? path : parts[parts.length - 1],
      attributes.getType() == FileMode.Type.DIRECTORY,
      false,
      attributes.getMtime(),
      attributes.getMtime(),
      attributes.getAtime(),
      attributes.getSize());
  }

  protected <T> T executeWithClient(@NonNull CheckedFunction1<SFTPClient, T> handler, T def) {
    // only take a client & execute the action if the pool is still available
    if (this.pool.stillActive()) {
      try (var client = this.pool.takeClient()) {
        return handler.apply(client);
      } catch (IllegalStateException exception) {
        if (exception.getCause() instanceof SSHException sshException) {
          LOGGER.error("Failed to retrieve a new client from the SFTP client pool: {}", sshException.getMessage());
        }
      } catch (Throwable exception) {
        LOGGER.debug("Exception executing sftp task", exception);
      }
    }

    // either an exception was thrown or the pool is closed
    return def;
  }
}
