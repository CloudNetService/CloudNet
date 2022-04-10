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

package eu.cloudnetservice.node.module;

import java.util.Collection;
import lombok.NonNull;

public record ModuleEntry(
  boolean official,
  @NonNull String name,
  @NonNull String website,
  @NonNull String version,
  @NonNull String sha3256,
  @NonNull String description,
  @NonNull String url,
  @NonNull Collection<String> maintainers,
  @NonNull Collection<String> releaseNotes,
  @NonNull Collection<String> dependingModules
) {

  @Override
  public @NonNull String url() {
    return this.url(
      System.getProperty("cloudnet.updateRepo", "CloudNetService/launchermeta"),
      System.getProperty("cloudnet.updateBranch", "release"));
  }

  public @NonNull String url(@NonNull String updaterRepo, @NonNull String updaterBranch) {
    return this.url.replace("%updateRepo%", updaterRepo).replace("%updateBranch%", updaterBranch);
  }
}
