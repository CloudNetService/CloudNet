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

package eu.cloudnetservice.modules.bridge.config;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record ProxyFallbackConfiguration(
  @NonNull String targetGroup,
  @Nullable String defaultFallbackTask,
  @NonNull List<ProxyFallback> fallbacks
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ProxyFallbackConfiguration configuration) {
    return builder()
      .targetGroup(configuration.targetGroup())
      .defaultFallbackTask(configuration.defaultFallbackTask())
      .fallbacks(configuration.fallbacks());
  }

  public static class Builder {

    private String targetGroup;
    private String defaultFallbackTask;
    private List<ProxyFallback> fallbacks = new ArrayList<>();

    public @NonNull Builder targetGroup(@NonNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NonNull Builder defaultFallbackTask(@Nullable String defaultFallbackTask) {
      this.defaultFallbackTask = defaultFallbackTask;
      return this;
    }

    public @NonNull Builder fallbacks(@NonNull List<ProxyFallback> fallbacks) {
      this.fallbacks = new ArrayList<>(fallbacks);
      return this;
    }

    public @NonNull ProxyFallbackConfiguration build() {
      Preconditions.checkNotNull(this.targetGroup, "Missing targetGroup");

      return new ProxyFallbackConfiguration(this.targetGroup, this.defaultFallbackTask, this.fallbacks);
    }
  }

}
