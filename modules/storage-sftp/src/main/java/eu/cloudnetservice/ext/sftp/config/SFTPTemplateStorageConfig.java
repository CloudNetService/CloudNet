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

package eu.cloudnetservice.ext.sftp.config;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class SFTPTemplateStorageConfig {

  private final HostAndPort address;

  private final String storage;
  private final String username;
  private final String password;

  private final Path sshKeyPath;
  private final String sshKeyPassword;

  private final Path knownHostFile;
  private final String baseDirectory;

  private final int clientPoolSize;

  public SFTPTemplateStorageConfig() {
    this(new HostAndPort("127.0.0.1", 22), "sftp", "root", "super_secret_key", null, null, null, "/home/cloudnet", 4);
  }

  public SFTPTemplateStorageConfig(
    @NotNull HostAndPort address,
    @NotNull String storage,
    @NotNull String username,
    @Nullable String password,
    @Nullable Path sshKeyPath,
    @Nullable String sshKeyPassword,
    @Nullable Path knownHostFile,
    @NotNull String baseDirectory,
    int clientPoolSize
  ) {
    this.address = address;
    this.storage = storage;
    this.username = username;
    this.password = password;
    this.sshKeyPath = sshKeyPath;
    this.sshKeyPassword = sshKeyPassword;
    this.knownHostFile = knownHostFile;
    this.baseDirectory = baseDirectory;
    this.clientPoolSize = clientPoolSize;
  }

  public @NotNull HostAndPort address() {
    return this.address;
  }

  public @NotNull String storage() {
    return this.storage;
  }

  public @NotNull String username() {
    return this.username;
  }

  public @UnknownNullability String password() {
    return this.password;
  }

  public @UnknownNullability Path sshKeyPath() {
    return this.sshKeyPath;
  }

  public @UnknownNullability String sshKeyPassword() {
    return this.sshKeyPassword;
  }

  public @Nullable Path knownHostFile() {
    return this.knownHostFile;
  }

  public @NotNull String baseDirectory() {
    return this.baseDirectory;
  }

  public int clientPoolSize() {
    return this.clientPoolSize;
  }
}
