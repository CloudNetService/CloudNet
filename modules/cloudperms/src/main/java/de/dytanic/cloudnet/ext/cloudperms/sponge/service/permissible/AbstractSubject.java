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

package de.dytanic.cloudnet.ext.cloudperms.sponge.service.permissible;

import de.dytanic.cloudnet.driver.permission.IPermissible;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.Permission;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

public abstract class AbstractSubject<T extends IPermissible> implements Subject {

  protected final T data;
  protected final String identifier;
  protected final SubjectCollection source;
  protected final IPermissionManagement permissionManagement;

  public AbstractSubject(String identifier, SubjectCollection source, T data, IPermissionManagement management) {
    this.identifier = identifier;
    this.source = source;
    this.data = data;
    this.permissionManagement = management;
  }

  @Override
  public SubjectCollection containingCollection() {
    return null;
  }

  @Override
  public SubjectReference asSubjectReference() {
    return this.source.newSubjectReference(this.identifier);
  }

  @Override
  public Optional<?> associatedObject() {
    return Optional.ofNullable(this.data);
  }

  @Override
  public Tristate permissionValue(String permission, Cause cause) {
    return this.getPermissionValue(Permission.of(permission));
  }

  @Override
  public Tristate permissionValue(String permission, Set<Context> contexts) {
    return this.getPermissionValue(Permission.of(permission));
  }

  @Override
  public boolean isChildOf(SubjectReference parent, Cause cause) {
    return this.isChild(parent.subjectIdentifier());
  }

  @Override
  public boolean isChildOf(SubjectReference parent, Set<Context> contexts) {
    return this.isChild(parent.subjectIdentifier());
  }

  @Override
  public List<? extends SubjectReference> parents(Cause cause) {
    return this.getParents();
  }

  @Override
  public List<? extends SubjectReference> parents(Set<Context> contexts) {
    return this.getParents();
  }

  @Override
  public Optional<String> option(String key, Cause cause) {
    return this.findOption(key);
  }

  @Override
  public Optional<String> option(String key, Set<Context> contexts) {
    return this.findOption(key);
  }

  @Override
  public String identifier() {
    return this.identifier;
  }

  protected @NotNull Optional<String> findOption(@NotNull String key) {
    return Optional.ofNullable(this.data.getProperties().getString(key));
  }

  protected @NotNull Tristate getPermissionValue(@NotNull Permission permission) {
    var result = this.permissionManagement.getPermissionResult(this.data, permission);
    return switch (result) {
      case ALLOWED -> Tristate.TRUE;
      case DENIED -> Tristate.FALSE;
      case FORBIDDEN -> Tristate.UNDEFINED;
    };
  }

  protected abstract boolean isChild(@NotNull String parent);

  protected abstract @NotNull List<? extends SubjectReference> getParents();
}
