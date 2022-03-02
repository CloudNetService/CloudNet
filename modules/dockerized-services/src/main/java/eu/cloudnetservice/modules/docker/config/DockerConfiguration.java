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

}
