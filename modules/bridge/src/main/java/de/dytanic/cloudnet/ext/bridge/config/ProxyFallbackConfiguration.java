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

package de.dytanic.cloudnet.ext.bridge.config;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
public final class ProxyFallbackConfiguration {

  private final String targetGroup;
  private final String defaultFallbackTask;

  private final List<ProxyFallback> fallbacks;

  public ProxyFallbackConfiguration(
    @NotNull String targetGroup,
    @Nullable String defaultFallbackTask,
    @NotNull List<ProxyFallback> fallbacks
  ) {
    this.targetGroup = targetGroup;
    this.defaultFallbackTask = defaultFallbackTask;
    this.fallbacks = fallbacks;
  }

  public @NotNull String getTargetGroup() {
    return this.targetGroup;
  }

  public @Nullable String getDefaultFallbackTask() {
    return this.defaultFallbackTask;
  }

  public @NotNull List<ProxyFallback> getFallbacks() {
    return this.fallbacks;
  }
}
