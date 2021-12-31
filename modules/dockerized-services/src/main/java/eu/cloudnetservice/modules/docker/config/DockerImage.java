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

import com.google.common.base.Verify;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record DockerImage(
  @NonNull String repository,
  @Nullable String tag,
  @Nullable String registry,
  @Nullable String platform
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public @NonNull String imageName() {
    // append the tag if given, if not docker will just use the latest image
    return this.repository + (this.tag == null ? "" : String.format(":%s", this.tag));
  }

  public static final class Builder {

    private String repository;
    private String tag;
    private String registry;
    private String platform;

    public @NonNull Builder repository(@NonNull String repository) {
      this.repository = repository;
      return this;
    }

    public @NonNull Builder tag(@Nullable String tag) {
      this.tag = tag;
      return this;
    }

    public @NonNull Builder registry(@Nullable String registry) {
      this.registry = registry;
      return this;
    }

    public @NonNull Builder platform(@Nullable String platform) {
      this.platform = platform;
      return this;
    }

    public @NonNull DockerImage build() {
      Verify.verifyNotNull(this.repository, "no repository given");
      return new DockerImage(this.repository, this.tag, this.registry, this.platform);
    }
  }
}
