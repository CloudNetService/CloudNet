/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudperms.sponge.service.memory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.service.permission.TransferMethod;
import org.spongepowered.api.util.Tristate;

final class InMemorySubjectData implements SubjectData {

  private final Subject owner;
  private final Map<Set<Context>, Map<String, String>> options;
  private final Map<Set<Context>, Tristate> fallbackPermissions;
  private final Map<Set<Context>, Map<String, Boolean>> permissions;

  public InMemorySubjectData(Subject owner) {
    this.owner = owner;
    this.options = new HashMap<>();
    this.fallbackPermissions = new HashMap<>();
    this.permissions = new HashMap<>();
  }

  @Override
  public Subject subject() {
    return this.owner;
  }

  @Override
  public boolean isTransient() {
    return false;
  }

  @Override
  public Map<Set<Context>, Map<String, Boolean>> allPermissions() {
    return this.permissions;
  }

  @Override
  public Map<String, Boolean> permissions(Set<Context> contexts) {
    return this.permissions.getOrDefault(contexts, Collections.emptyMap());
  }

  @Override
  public CompletableFuture<Boolean> setPermission(Set<Context> contexts, String permission, Tristate value) {
    return CompletableFuture.completedFuture(
      this.permissions.computeIfAbsent(contexts, $ -> new HashMap<>()).put(permission, value.asBoolean()));
  }

  @Override
  public CompletableFuture<Boolean> setPermissions(
    Set<Context> contexts,
    Map<String, Boolean> permissions,
    TransferMethod method
  ) {
    this.permissions.computeIfAbsent(contexts, $ -> new HashMap<>()).putAll(permissions);
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public Tristate fallbackPermissionValue(Set<Context> contexts) {
    return this.fallbackPermissions.getOrDefault(contexts, Tristate.FALSE);
  }

  @Override
  public Map<Set<Context>, Tristate> allFallbackPermissionValues() {
    return this.fallbackPermissions;
  }

  @Override
  public CompletableFuture<Boolean> setFallbackPermissionValue(Set<Context> contexts, Tristate fallback) {
    this.fallbackPermissions.put(contexts, fallback);
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> clearFallbackPermissionValues() {
    this.fallbackPermissions.clear();
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> clearPermissions() {
    this.permissions.clear();
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> clearPermissions(Set<Context> contexts) {
    this.permissions.remove(contexts);
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public Map<Set<Context>, ? extends List<? extends SubjectReference>> allParents() {
    return Collections.emptyMap();
  }

  @Override
  public List<? extends SubjectReference> parents(Set<Context> contexts) {
    return Collections.emptyList();
  }

  @Override
  public CompletableFuture<Boolean> setParents(
    Set<Context> contexts,
    List<? extends SubjectReference> parents,
    TransferMethod method
  ) {
    return CompletableFuture.completedFuture(false);
  }

  @Override
  public CompletableFuture<Boolean> addParent(Set<Context> contexts, SubjectReference parent) {
    return CompletableFuture.completedFuture(false);
  }

  @Override
  public CompletableFuture<Boolean> removeParent(Set<Context> contexts, SubjectReference parent) {
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> clearParents() {
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> clearParents(Set<Context> contexts) {
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public Map<Set<Context>, Map<String, String>> allOptions() {
    return this.options;
  }

  @Override
  public Map<String, String> options(Set<Context> contexts) {
    return this.options.getOrDefault(contexts, Collections.emptyMap());
  }

  @Override
  public CompletableFuture<Boolean> setOption(Set<Context> contexts, String key, @Nullable String value) {
    this.options.computeIfAbsent(contexts, $ -> new HashMap<>()).put(key, value);
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> setOptions(Set<Context> contexts, Map<String, String> options, TransferMethod $) {
    this.options.computeIfAbsent(contexts, $1 -> new HashMap<>()).putAll(options);
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> clearOptions() {
    this.options.clear();
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> clearOptions(Set<Context> contexts) {
    this.options.getOrDefault(contexts, Collections.emptyMap()).clear();
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> copyFrom(SubjectData other, TransferMethod method) {
    this.options.putAll(other.allOptions());
    this.permissions.putAll(other.allPermissions());
    this.fallbackPermissions.putAll(other.allFallbackPermissionValues());

    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> moveFrom(SubjectData other, TransferMethod method) {
    return this.copyFrom(other, method);
  }
}
