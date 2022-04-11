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
import java.util.HashSet;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record TaskDockerConfig(
  @Nullable DockerImage javaImage,
  @NonNull Set<String> volumes,
  @NonNull Set<String> binds,
  @NonNull Set<ExposedPort> exposedPorts
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull TaskDockerConfig config) {
    return builder()
      .javaImage(config.javaImage())
      .volumes(config.volumes())
      .binds(config.binds())
      .exposedPorts(config.exposedPorts());
  }

  public static class Builder {

    private DockerImage javaImage;
    private Set<String> volumes = new HashSet<>();
    private Set<String> binds = new HashSet<>();
    private Set<ExposedPort> exposedPorts = new HashSet<>();

    public @NonNull Builder javaImage(@Nullable DockerImage javaImage) {
      this.javaImage = javaImage;
      return this;
    }

    public @NonNull Builder addVolume(@NonNull String volume) {
      this.volumes.add(volume);
      return this;
    }

    public @NonNull Builder volumes(@NonNull Set<String> volumes) {
      this.volumes = new HashSet<>(volumes);
      return this;
    }

    public @NonNull Builder addBind(@NonNull String bind) {
      this.binds.add(bind);
      return this;
    }

    public @NonNull Builder binds(@NonNull Set<String> binds) {
      this.binds = new HashSet<>(binds);
      return this;
    }

    public @NonNull Builder addExposedPort(@NonNull ExposedPort port) {
      this.exposedPorts.add(port);
      return this;
    }

    public @NonNull Builder exposedPorts(@NonNull Set<ExposedPort> exposedPorts) {
      this.exposedPorts = new HashSet<>(exposedPorts);
      return this;
    }

    public @NonNull TaskDockerConfig build() {
      return new TaskDockerConfig(this.javaImage, this.volumes, this.binds, this.exposedPorts);
    }
  }
}
