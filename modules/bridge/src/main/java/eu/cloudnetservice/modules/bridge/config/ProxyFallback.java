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
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record ProxyFallback(
  int priority,
  @NonNull String task,
  @Nullable String permission,
  @Nullable String forcedHost,
  @NonNull Collection<String> availableOnGroups
) implements Comparable<ProxyFallback> {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ProxyFallback fallback) {
    return builder()
      .priority(fallback.priority())
      .task(fallback.task())
      .permission(fallback.permission())
      .forcedHost(fallback.forcedHost())
      .availableOnGroups(fallback.availableOnGroups());
  }

  @Override
  public int compareTo(@NonNull ProxyFallback o) {
    return Integer.compare(o.priority, this.priority);
  }

  public static class Builder {

    private int priority;

    private String task;
    private String permission;
    private String forcedHost;

    private Collection<String> availableOnGroups = new ArrayList<>();

    public @NonNull Builder priority(int priority) {
      this.priority = priority;
      return this;
    }

    public @NonNull Builder task(@NonNull String task) {
      this.task = task;
      return this;
    }

    public @NonNull Builder permission(@Nullable String permission) {
      this.permission = permission;
      return this;
    }

    public @NonNull Builder forcedHost(@Nullable String forcedHost) {
      this.forcedHost = forcedHost;
      return this;
    }

    public @NonNull Builder availableOnGroups(@NonNull Collection<String> availableOnGroups) {
      this.availableOnGroups = new ArrayList<>(availableOnGroups);
      return this;
    }

    public @NonNull ProxyFallback build() {
      Preconditions.checkNotNull(this.task, "Missing task");

      return new ProxyFallback(this.priority, this.task, this.permission, this.forcedHost, this.availableOnGroups);
    }
  }
}
