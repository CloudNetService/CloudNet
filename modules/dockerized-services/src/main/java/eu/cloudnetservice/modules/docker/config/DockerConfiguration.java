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

package eu.cloudnetservice.modules.docker.config;

import com.github.dockerjava.api.model.ExposedPort;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record DockerConfiguration(
  @NonNull String factoryName,
  @NonNull String network,
  @NonNull DockerImage javaImage,
  @NonNull Set<String> volumes,
  @NonNull Set<String> binds,
  @NonNull Set<ExposedPort> exposedPorts,
  @NonNull String dockerHost,
  @Nullable String dockerCertPath,
  @Nullable String registryUsername,
  @Nullable String registryEmail,
  @Nullable String registryPassword,
  @Nullable String registryUrl,
  @Nullable String user
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull DockerConfiguration configuration) {
    return builder()
      .factoryName(configuration.factoryName())
      .network(configuration.network())
      .javaImage(configuration.javaImage())
      .volumes(configuration.volumes())
      .binds(configuration.binds())
      .exposedPorts(configuration.exposedPorts())
      .dockerHost(configuration.dockerHost())
      .dockerCertPath(configuration.dockerCertPath())
      .registryUsername(configuration.registryUsername())
      .registryEmail(configuration.registryEmail())
      .registryPassword(configuration.registryPassword())
      .registryUrl(configuration.registryUrl())
      .user(configuration.user());
  }

  public static final class Builder {

    private DockerImage javaImage;

    private String network = "host";
    private String factoryName = "docker-jvm";
    private String dockerHost = "unix:///var/run/docker.sock";

    private Set<String> volumes = new HashSet<>();
    private Set<String> binds = new HashSet<>();
    private Set<ExposedPort> exposedPorts = new HashSet<>();

    private String dockerCertPath;
    private String registryUsername;
    private String registryEmail;
    private String registryPassword;
    private String registryUrl;
    private String user;

    public @NonNull Builder javaImage(@NonNull DockerImage javaImage) {
      this.javaImage = javaImage;
      return this;
    }

    public @NonNull Builder network(@NonNull String network) {
      this.network = network;
      return this;
    }

    public @NonNull Builder factoryName(@NonNull String factoryName) {
      this.factoryName = factoryName;
      return this;
    }

    public @NonNull Builder dockerHost(@NonNull String dockerHost) {
      this.dockerHost = dockerHost;
      return this;
    }

    public @NonNull Builder volumes(@NonNull Collection<String> volumes) {
      this.volumes = new HashSet<>(volumes);
      return this;
    }

    public @NonNull Builder addVolume(@NonNull String volume) {
      this.volumes.add(volume);
      return this;
    }

    public @NonNull Builder binds(@NonNull Collection<String> binds) {
      this.binds = new HashSet<>(binds);
      return this;
    }

    public @NonNull Builder addBind(@NonNull String bind) {
      this.binds.add(bind);
      return this;
    }

    public @NonNull Builder exposedPorts(@NonNull Collection<ExposedPort> exposedPorts) {
      this.exposedPorts = new HashSet<>(exposedPorts);
      return this;
    }

    public @NonNull Builder addExposedPort(@NonNull ExposedPort port) {
      this.exposedPorts.add(port);
      return this;
    }

    public @NonNull Builder dockerCertPath(@Nullable String dockerCertPath) {
      this.dockerCertPath = dockerCertPath;
      return this;
    }

    public @NonNull Builder registryUsername(@Nullable String registryUsername) {
      this.registryUsername = registryUsername;
      return this;
    }

    public @NonNull Builder registryEmail(@Nullable String registryEmail) {
      this.registryEmail = registryEmail;
      return this;
    }

    public @NonNull Builder registryPassword(@Nullable String registryPassword) {
      this.registryPassword = registryPassword;
      return this;
    }

    public @NonNull Builder registryUrl(@Nullable String registryUrl) {
      this.registryUrl = registryUrl;
      return this;
    }

    public @NonNull Builder user(@Nullable String user) {
      this.user = user;
      return this;
    }

    public @NonNull DockerConfiguration build() {
      Preconditions.checkNotNull(this.javaImage, "Java docker image must be given");
      return new DockerConfiguration(
        this.factoryName,
        this.network,
        this.javaImage,
        this.volumes,
        this.binds,
        this.exposedPorts,
        this.dockerHost,
        this.dockerCertPath,
        this.registryUsername,
        this.registryEmail,
        this.registryPassword,
        this.registryUrl,
        this.user);
    }
  }
}
