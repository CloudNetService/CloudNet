/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.module.locator;

import eu.cloudnetservice.common.Named;
import java.util.List;
import lombok.NonNull;

/**
 * A locator for possible module files, from any source. Module resources must be representable by a path.
 *
 * @param <R> the type of module resources that are resolved by this locator.
 * @since 4.0
 */
public interface ModuleLocator<R extends ModuleResource> extends Named {

  /**
   * Locates all possible module resources. This locator is not required to make a check if a resource can actually be
   * loaded as a module, these checks are made at a later stage.
   *
   * @return the located module resources regardless if they will be actually loadable as a module.
   */
  @NonNull
  List<R> locateModuleResources();
}
