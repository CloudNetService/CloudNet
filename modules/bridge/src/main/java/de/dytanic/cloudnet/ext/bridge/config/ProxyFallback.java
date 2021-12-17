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

import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
public final class ProxyFallback implements Comparable<ProxyFallback> {

  private final int priority;

  private final String task;
  private final String permission;
  private final String forcedHost;

  private final Collection<String> availableOnGroups;

  public ProxyFallback(int priority, @NotNull String task, @Nullable String permission, @Nullable String forcedHost) {
    this(priority, task, permission, forcedHost, new ArrayList<>());
  }

  public ProxyFallback(
    int priority,
    @NotNull String task,
    @Nullable String permission,
    @Nullable String forcedHost,
    @NotNull Collection<String> availableOnGroups
  ) {
    this.priority = priority;
    this.task = task;
    this.permission = permission;
    this.forcedHost = forcedHost;
    this.availableOnGroups = availableOnGroups;
  }

  public @NotNull String task() {
    return this.task;
  }

  public @Nullable String permission() {
    return this.permission;
  }

  public @NotNull Collection<String> availableOnGroups() {
    return this.availableOnGroups;
  }

  public @Nullable String forcedHost() {
    return this.forcedHost;
  }

  public int priority() {
    return this.priority;
  }

  @Override
  public int compareTo(@NotNull ProxyFallback o) {
    return Integer.compare(o.priority, this.priority);
  }
}
