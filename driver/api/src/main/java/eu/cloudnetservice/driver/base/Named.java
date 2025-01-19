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

package eu.cloudnetservice.driver.base;

import lombok.NonNull;

/**
 * Identifies that the object implementing this interface can be identified using the name supplied by the
 * {@link #name()} method. There is guarantee that the given name is unique, however a nameable object must always be
 * unique in a specific scope. This means that there might be a requirement for multiple parameters to be used in order
 * to use the supplied name as an identifier.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface Named {

  /**
   * Get the unique name identifier of this object in the related scope.
   *
   * @return the unique name identifier of this object in the related scope.
   */
  @NonNull String name();
}
