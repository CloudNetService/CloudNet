/*
<<<<<<< HEAD:driver/api/src/main/java/eu/cloudnetservice/driver/module/ModuleCandidate.java
 * Copyright 2019-present CloudNetService team & contributors
=======
 * Copyright 2019-2025 CloudNetService team & contributors
>>>>>>> 2762714fa (move stuff):node/api/src/main/java/eu/cloudnetservice/node/module/ModuleCandidate.java
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

package eu.cloudnetservice.node.module;

import eu.cloudnetservice.node.module.locator.ModuleResource;
import eu.cloudnetservice.node.module.metadata.ModuleMetadata;
import lombok.NonNull;

/**
 * A candidate of a module that is not yet loaded, but passed the first checks if it could be loaded.
 *
 * @since 4.0
 */
public interface ModuleCandidate<R extends ModuleResource> {

  /**
   * Get the resource which is the basis of this candidate.
   *
   * @return the resource which is the basis of this candidate.
   */
  @NonNull
  R resource();

  /**
   * Get the module metadata which was parsed from the resolved module resource.
   *
   * @return the module metadata which was parsed from the resolved module resource.
   */
  @NonNull
  ModuleMetadata metadata();
}
