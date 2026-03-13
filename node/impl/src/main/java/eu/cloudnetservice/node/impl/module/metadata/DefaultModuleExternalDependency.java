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
import java.util.Collection;
import lombok.NonNull;

/**
 * Default implementation of an external module dependency.
 *
 * @param loader         the name of the loader to use for the dependency.
 * @param environments   the environments the dependency should be copied to.
 * @param propertyHolder the additional properties of the dependency.
 * @param optional       if the dependency is optional, meaning that it can be ignored if loading fails.
 * @since 4.0
 */
record DefaultModuleExternalDependency(
  @NonNull String loader,
  @NonNull Collection<String> environments,
  @NonNull Document propertyHolder,
  boolean optional
) implements ModuleExternalDependency, DefaultedDocPropertyHolder {

}
