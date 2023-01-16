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

package eu.cloudnetservice.modules.cloudperms.sponge.service.permissible;

import eu.cloudnetservice.driver.permission.Permissible;
import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.service.permission.TransferMethod;
import org.spongepowered.api.util.Tristate;

public abstract class PermissibleSubjectData<T extends Permissible> implements SubjectData {

  private static final Map<Set<Context>, Tristate> UNDEFINED = Map.of(
    Collections.emptySet(),
    Tristate.UNDEFINED);

  protected final boolean allowModify;

  protected final T permissible;
  protected final Subject subject;
  protected final PermissionManagement management;

  public PermissibleSubjectData(
    boolean allowModify,
    T permissible,
    Subject subject,
    PermissionManagement management
  ) {
    this.allowModify = allowModify;
    this.permissible = permissible;
    this.subject = subject;
    this.management = management;
  }

  @Override
  public Subject subject() {
    return this.subject;
  }

  @Override
  public boolean isTransient() {
    return !this.allowModify;
  }

  @Override
  public Map<Set<Context>, Map<String, Boolean>> allPermissions() {
    return this.management.allPermissions(this.permissible).stream().collect(
      Collectors.collectingAndThen(
        Collectors.toMap(Permission::name, perm -> perm.potency() >= 0),
        result -> Map.of(Collections.emptySet(), result)));
  }

  @Override
  public Map<String, Boolean> permissions(Set<Context> contexts) {
    return this.management.allPermissions(this.permissible).stream().collect(Collectors.toMap(
      Permission::name,
      perm -> perm.potency() >= 0));
  }

  @Override
  public CompletableFuture<Boolean> setPermission(Set<Context> contexts, String permission, Tristate value) {
    return CompletableFuture.supplyAsync(() -> {
      // unset the permission if the Tristate is undefined as described in the java docs
      if (value == Tristate.UNDEFINED) {
        this.permissible.removePermission(permission);
      } else {
        this.permissible.addPermission(
          Permission.builder().name(permission).potency(value.asBoolean() ? 1 : -1).build());
      }
      // update
      this.updateIfEnabled(this.permissible);
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> setPermissions(Set<Context> $, Map<String, Boolean> perms, TransferMethod $1) {
    return CompletableFuture.supplyAsync(() -> {
      // set each permission
      perms.forEach((perm, value) -> this.permissible.addPermission(
        Permission.builder().name(perm).potency(value ? 1 : -1).build()));
      this.updateIfEnabled(this.permissible);
      return true;
    });
  }

  @Override
  public Tristate fallbackPermissionValue(Set<Context> contexts) {
    return Tristate.UNDEFINED;
  }

  @Override
  public Map<Set<Context>, Tristate> allFallbackPermissionValues() {
    return UNDEFINED;
  }

  @Override
  public CompletableFuture<Boolean> setFallbackPermissionValue(Set<Context> contexts, Tristate fallback) {
    return CompletableFuture.completedFuture(true); // not supported
  }

  @Override
  public CompletableFuture<Boolean> clearFallbackPermissionValues() {
    return CompletableFuture.completedFuture(true); // not supported
  }

  @Override
  public CompletableFuture<Boolean> clearPermissions() {
    return CompletableFuture.supplyAsync(() -> {
      this.permissible.permissions().forEach(perm -> this.permissible.removePermission(perm.name()));
      this.updateIfEnabled(this.permissible);
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> clearPermissions(Set<Context> contexts) {
    return this.clearPermissions();
  }

  @Override
  public Map<Set<Context>, ? extends List<? extends SubjectReference>> allParents() {
    return null;
  }

  @Override
  public List<? extends SubjectReference> parents(Set<Context> contexts) {
    return this.allParents().get(Collections.emptySet());
  }

  @Override
  public CompletableFuture<Boolean> clearParents(Set<Context> contexts) {
    return this.clearParents();
  }

  @Override
  public Map<Set<Context>, Map<String, String>> allOptions() {
    return Map.of(Collections.emptySet(), this.permissible.properties().stream()
      .filter(key -> this.permissible.properties().getString(key) != null)
      .collect(Collectors.toMap(Function.identity(), this.permissible.properties()::getString)));
  }

  @Override
  public Map<String, String> options(Set<Context> contexts) {
    return this.allOptions().get(Collections.emptySet());
  }

  @Override
  public CompletableFuture<Boolean> setOption(Set<Context> contexts, String key, @Nullable String value) {
    return CompletableFuture.supplyAsync(() -> {
      this.permissible.properties().append(key, value);
      this.updateIfEnabled(this.permissible);
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> setOptions(Set<Context> $, Map<String, String> options, TransferMethod $1) {
    return CompletableFuture.supplyAsync(() -> {
      options.forEach(this.permissible.properties()::append);
      this.updateIfEnabled(this.permissible);
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> clearOptions() {
    return CompletableFuture.supplyAsync(() -> {
      this.permissible.properties().clear();
      this.updateIfEnabled(this.permissible);
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> clearOptions(Set<Context> contexts) {
    return this.clearOptions();
  }

  @Override
  public CompletableFuture<Boolean> copyFrom(SubjectData other, TransferMethod method) {
    return CompletableFuture.completedFuture(true); // not supported
  }

  @Override
  public CompletableFuture<Boolean> moveFrom(SubjectData other, TransferMethod method) {
    return CompletableFuture.completedFuture(true); // not supported
  }

  protected void updateIfEnabled(@NonNull T data) {
    if (this.allowModify) {
      this.update(data);
    }
  }

  protected abstract void update(@NonNull T data);
}
