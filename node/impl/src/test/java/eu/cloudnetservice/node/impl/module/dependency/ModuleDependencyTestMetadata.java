/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module.dependency;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.node.impl.module.metadata.UnknownModuleVersion;
import eu.cloudnetservice.node.module.dependency.ModuleExternalDependency;
import eu.cloudnetservice.node.module.metadata.ModuleArtifact;
import eu.cloudnetservice.node.module.metadata.ModuleContributor;
import eu.cloudnetservice.node.module.metadata.ModuleDependency;
import eu.cloudnetservice.node.module.metadata.ModuleMetadata;
import eu.cloudnetservice.node.module.metadata.ModuleVersion;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

public record ModuleDependencyTestMetadata(
  @NonNull String id,
  @NonNull Collection<ModuleDependency> moduleDependencies
) implements ModuleMetadata, DefaultedDocPropertyHolder {

  public ModuleDependencyTestMetadata(@NonNull String id, @NonNull Set<String> dependencyModuleIds) {
    var moduleDependencies = dependencyModuleIds.stream()
      .map(moduleId -> new ModuleDependency(moduleId, "*", ModuleDependency.DependencyType.REQUIRED))
      .toList();
    this(id, moduleDependencies);
  }

  @Override
  public @NonNull String displayName() {
    return this.id;
  }

  @Override
  public @NonNull String description() {
    return "Test Module " + this.id;
  }

  @Override
  public @NonNull String entrypoint() {
    return "";
  }

  @Override
  public @NonNull ModuleVersion version() {
    return UnknownModuleVersion.parse("0.0.0");
  }

  @Override
  public @NonNull @Unmodifiable Collection<String> licenses() {
    return List.of();
  }

  @Override
  public @NonNull @Unmodifiable Collection<ModuleArtifact> artifacts() {
    return List.of();
  }

  @Override
  public @NonNull @Unmodifiable Collection<ModuleExternalDependency> externalDependencies() {
    return List.of();
  }

  @Override
  public @NonNull @Unmodifiable Collection<ModuleContributor> authors() {
    return List.of();
  }

  @Override
  public @NonNull @Unmodifiable Collection<ModuleContributor> contributors() {
    return List.of();
  }

  @Override
  public @NonNull Document propertyHolder() {
    return Document.emptyDocument();
  }
}
