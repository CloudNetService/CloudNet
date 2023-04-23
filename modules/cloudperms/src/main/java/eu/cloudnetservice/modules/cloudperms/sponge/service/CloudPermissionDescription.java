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

package eu.cloudnetservice.modules.cloudperms.sponge.service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.plugin.PluginContainer;

final class CloudPermissionDescription implements PermissionDescription {

  private final PluginContainer owner;
  private final CloudPermsPermissionService permService;

  private String id;
  private Component desc;
  private Tristate defaultValue;

  public CloudPermissionDescription(PluginContainer owner, CloudPermsPermissionService permService) {
    this.owner = owner;
    this.permService = permService;
  }

  @Override
  public String id() {
    return this.id;
  }

  @Override
  public Optional<Component> description() {
    return Optional.ofNullable(this.desc);
  }

  @Override
  public Optional<PluginContainer> owner() {
    return Optional.ofNullable(this.owner);
  }

  @Override
  public Tristate defaultValue() {
    return this.defaultValue;
  }

  @Override
  public CompletableFuture<? extends Map<? extends SubjectReference, Boolean>> findAssignedSubjects(String id) {
    return CompletableFuture.completedFuture(Collections.emptyMap());
  }

  @Override
  public Map<? extends Subject, Boolean> assignedSubjects(String collectionIdentifier) {
    return Collections.emptyMap();
  }

  @Override
  public boolean query(Subject subj) {
    return false;
  }

  @Override
  public boolean query(Subject subj, ResourceKey key) {
    return false;
  }

  @Override
  public boolean query(Subject subj, String... parameters) {
    return false;
  }

  @Override
  public boolean query(Subject subj, String parameter) {
    return false;
  }

  public static final class CloudPermissionDescriptionBuilder implements Builder {

    private final CloudPermissionDescription description;

    public CloudPermissionDescriptionBuilder(PluginContainer owner, CloudPermsPermissionService permService) {
      this.description = new CloudPermissionDescription(owner, permService);
    }

    @Override
    public Builder id(String permissionId) {
      this.description.id = permissionId;
      return this;
    }

    @Override
    public Builder description(@Nullable Component description) {
      this.description.desc = description;
      return this;
    }

    @Override
    public Builder assign(String role, boolean value) {
      return this; // no-op
    }

    @Override
    public Builder defaultValue(Tristate defaultValue) {
      this.description.defaultValue = defaultValue;
      return this;
    }

    @Override
    public PermissionDescription register() throws IllegalStateException {
      this.description.permService.descriptions.add(this.description);
      return this.description;
    }
  }
}
