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

package eu.cloudnetservice.node.impl.module.metadata;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.node.module.dependency.ModuleExternalDependency;
import eu.cloudnetservice.node.module.metadata.ModuleArtifact;
import eu.cloudnetservice.node.module.metadata.ModuleContributor;
import eu.cloudnetservice.node.module.metadata.ModuleDependency;
import eu.cloudnetservice.node.module.metadata.ModuleMetadata;
import eu.cloudnetservice.node.module.metadata.ModuleVersion;
import java.util.Collection;
import lombok.NonNull;

/**
 * Metadata V1 implementation that is parsed from a JSON file by the {@link DefaultModuleMetadataParser}.
 *
 * @param id                   the id of the module.
 * @param displayName          the name of the module.
 * @param description          the description of the module.
 * @param entrypoint           the entrypoint of the module.
 * @param version              the parsed version of the module.
 * @param licenses             the licenses of the module, potentially empty.
 * @param artifacts            the artifacts provided by the module.
 * @param moduleDependencies   the dependencies on other modules by this module.
 * @param externalDependencies the external dependencies of this module on other modules.
 * @param authors              the authors of the module.
 * @param contributors         the contributors of the module.
 * @param propertyHolder       additional properties provided in the module metadata file.
 * @since 4.0
 */
record DefaultModuleMetadataV1(
  @NonNull String id,
  @NonNull String displayName,
  @NonNull String description,
  @NonNull String entrypoint,
  @NonNull ModuleVersion version,
  @NonNull Collection<String> licenses,
  @NonNull Collection<ModuleArtifact> artifacts,
  @NonNull Collection<ModuleDependency> moduleDependencies,
  @NonNull Collection<ModuleExternalDependency> externalDependencies,
  @NonNull Collection<ModuleContributor> authors,
  @NonNull Collection<ModuleContributor> contributors,
  @NonNull Document propertyHolder
) implements ModuleMetadata, DefaultedDocPropertyHolder {

}
