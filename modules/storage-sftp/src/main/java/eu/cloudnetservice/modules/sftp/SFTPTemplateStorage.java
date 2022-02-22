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

package eu.cloudnetservice.modules.sftp;

import eu.cloudnetservice.cloudnet.common.function.ThrowableFunction;
import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.common.io.ZipUtil;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.driver.template.FileInfo;
import eu.cloudnetservice.cloudnet.driver.template.TemplateStorage;
import eu.cloudnetservice.modules.sftp.config.SFTPTemplateStorageConfig;
import eu.cloudnetservice.modules.sftp.sshj.FilteringLocalFileSource;
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
import java.util.function.Predicate;
import lombok.NonNull;
import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.Config;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode.Type;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.jetbrains.annotations.Nullable;

public class SFTPTemplateStorage implements TemplateStorage {

  protected static final String REMOTE_DIR_FORMAT = "%s/%s/%s";
  protected static final Logger LOGGER = LogManager.logger(SFTPTemplateStorage.class);

  private final Config config;
  private final SFTPClientPool pool;
  private final SFTPTemplateStorageConfig storageConfig;

  private volatile SSHClient client;

  public SFTPTemplateStorage(@NonNull SFTPTemplateStorageConfig config) {
    this.storageConfig = config;
    // init the config
    this.config = new DefaultConfig();
    this.config.setLoggerFactory(NopLoggerFactory.INSTANCE);
    this.config.setKeepAliveProvider(KeepAliveProvider.HEARTBEAT);
    // init the pool
    this.pool = new SFTPClientPool(config.clientPoolSize(), () -> {
      var client = this.client;
      // check if the client was ever initialized
      if (client != null) {
        // check if the client is still connected
        if (client.isConnected() && client.isAuthenticated()) {
          // we can use the current client
          return client;
        } else {
          // the current client is not available anymore
          client.disconnect();
          this.client = null;
        }
      }
      // create and set a new client
      this.client = new SSHClient(this.config);
      this.client.setConnectTimeout(5000);
      this.client.setRemoteCharset(StandardCharsets.UTF_8);
      // load the known hosts file if given
      if (config.knownHostFile() == null) {
        // always trust the server
        this.client.addHostKeyVerifier(new PromiscuousVerifier());
      } else {
        // load the known hosts file
        this.client.loadKnownHosts(config.knownHostFile().toFile());
      }
      // connect to the server
      this.client.connect(config.address().host(), config.address().port());
      // authenticate the client with the correct auth method
      if (config.sshKeyPath() != null) {
        this.client.authPublickey(
          config.username(),
          this.client.loadKeys(config.sshKeyPath().toString(), config.sshKeyPassword()));
      } else {
        this.client.authPassword(config.username(), config.password());
      }
      // return the created client
      return this.client;
    });
  }

  @Override
  public @NonNull String name() {
    return this.storageConfig.storage();
  }

  @Override
  public boolean deployDirectory(
    @NonNull Path directory,
    @NonNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter
  ) {
    return this.executeWithClient(client -> {
      client.put(new FilteringLocalFileSource(directory, fileFilter), this.constructRemotePath(target));
      return true;
    }, false);
  }

  @Override
  public boolean deploy(
    @NonNull InputStream inputStream,
    @NonNull ServiceTemplate target
  ) {
    var temp = ZipUtil.extract(inputStream, FileUtil.createTempFile());
    if (temp != null) {
      try {
        return this.deployDirectory(temp, target, null);
      } finally {
        FileUtil.delete(temp);
      }
    }
    return false;
  }

  @Override
  public boolean copy(@NonNull ServiceTemplate template, @NonNull Path directory) {
    return this.executeWithClient(client -> {
      client.get(this.constructRemotePath(template), new FileSystemFile(directory.toFile()));
      return true;
    }, false);
  }

  @Override
  public @Nullable InputStream zipTemplate(@NonNull ServiceTemplate template) {
    return this.executeWithClient(client -> {
      var localTarget = FileUtil.createTempFile();
      if (this.copy(template, localTarget)) {
        return ZipUtil.zipToStream(localTarget);
      } else {
        return null;
      }
    }, null);
  }

  @Override
  public boolean delete(@NonNull ServiceTemplate template) {
    return this.executeWithClient(client -> {
      if (client.statExistence(this.constructRemotePath(template)) != null) {
        this.deleteDir(client, this.constructRemotePath(template));
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
  public boolean has(@NonNull ServiceTemplate template) {
    return this.executeWithClient(client -> {
      var attr = client.statExistence(this.constructRemotePath(template));
      return attr != null && attr.getType() == Type.DIRECTORY;
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
  public @Nullable FileInfo[] listFiles(@NonNull ServiceTemplate template, @NonNull String dir, boolean deep) {
    return this.executeWithClient(client -> {
      Set<FileInfo> result = new HashSet<>();
      this.ls(client, result, template, dir, deep);
      return result.toArray(new FileInfo[0]);
    }, null);
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
    this.client.disconnect();
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
      attributes.getType() == Type.DIRECTORY,
      false,
      attributes.getMtime(),
      attributes.getMtime(),
      attributes.getAtime(),
      attributes.getSize());
  }

  protected <T> T executeWithClient(@NonNull ThrowableFunction<SFTPClient, T, Exception> handler, T def) {
    try (SFTPClient client = this.pool.takeClient()) {
      return handler.apply(client);
    } catch (Exception exception) {
      LOGGER.fine("Exception executing sftp task", exception);
      return def;
    }
  }
}
