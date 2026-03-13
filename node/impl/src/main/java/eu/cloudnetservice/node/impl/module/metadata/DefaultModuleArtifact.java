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

import eu.cloudnetservice.node.module.metadata.ModuleArtifact;
import java.util.Collection;
import lombok.NonNull;

/**
 * Default implementation of a module artifact.
 *
 * @param source       the source where the artifacted is retrieved from.
 * @param sourcePath   the path within the source to resolve the artifact from. Always relative.
 * @param targetPath   the path to copy the artifact to. Always relative.
 * @param environments the environments to copy the artifact to.
 * @since 4.0
 */
record DefaultModuleArtifact(
  @NonNull Source source,
  @NonNull String sourcePath,
  @NonNull String targetPath,
  @NonNull Collection<String> environments
) implements ModuleArtifact {

}
