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

package de.dytanic.cloudnet.driver.service.property;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class helps getting/modifying properties in {@link ServiceInfoSnapshot}s.
 *
 * @param <T> the type of the value for this property
 */
public interface ServiceProperty<T> {

  /**
   * Gets a property out of the properties of the given {@link ServiceInfoSnapshot}.
   *
   * @param serviceInfoSnapshot the serviceInfoSnapshot to get the property
   * @return an optional with the value, might be empty
   */
  @NotNull
  Optional<T> get(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);

  /**
   * Sets a property into the properties of the given {@link ServiceInfoSnapshot}. Some properties might not support
   * this. An update of the service might be necessary.
   *
   * @param serviceInfoSnapshot the serviceInfoSnapshot to modify the property in
   * @param value               the value to set or null to remove the property
   */
  void set(@NotNull ServiceInfoSnapshot serviceInfoSnapshot, @Nullable T value);

}
