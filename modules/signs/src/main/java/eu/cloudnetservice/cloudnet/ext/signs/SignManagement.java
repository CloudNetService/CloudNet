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

package eu.cloudnetservice.cloudnet.ext.signs;

import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import java.util.Collection;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a shared management for the sign system.
 */
public interface SignManagement {

  /**
   * Get a sign which is located at the specified location.
   *
   * @param position the position the sign is located at
   * @return the sign at the given location or null if there is no sign
   */
  @Nullable Sign signAt(@NotNull WorldPosition position);

  /**
   * Creates a new sign.
   *
   * @param sign the sign to create
   */
  void createSign(@NotNull Sign sign);

  /**
   * Deletes the specified sign.
   *
   * @param sign the sign to delete.
   */
  void deleteSign(@NotNull Sign sign);

  /**
   * Deletes the sign at the given position.
   *
   * @param position the position of the sign to delete.
   */
  void deleteSign(@NotNull WorldPosition position);

  /**
   * Deletes all signs of the specified group.
   *
   * @param group the group to delete the signs of
   * @return the amount of deleted signs
   */
  int deleteAllSigns(@NotNull String group);

  /**
   * Deletes all signs of the specified group.
   *
   * @param group        the group to delete the signs of
   * @param templatePath the template path of the signs to delete
   * @return the amount of deleted signs
   */
  int deleteAllSigns(@NotNull String group, @Nullable String templatePath);

  /**
   * Deletes all signs.
   *
   * @return the amount of deleted signs
   */
  int deleteAllSigns();

  /**
   * Get all registered signs.
   *
   * @return all registered signs.
   */
  @NotNull Collection<Sign> signs();

  /**
   * Get all signs of the specified groups.
   *
   * @param groups the groups the signs are created on
   * @return all signs that are created on the given groups
   */
  @NotNull Collection<Sign> signs(@NotNull String[] groups);

  /**
   * Get the current sign configuration.
   *
   * @return the current sign configuration
   */
  @NotNull SignsConfiguration signsConfiguration();

  /**
   * Sets the sign configuration and updates it to all connected components.
   *
   * @param configuration the new signs configuration.
   */
  void signsConfiguration(@NotNull SignsConfiguration configuration);

  // Internal methods

  @Internal
  void registerToServiceRegistry();

  @Internal
  void unregisterFromServiceRegistry();

  @Internal
  void handleInternalSignCreate(@NotNull Sign sign);

  @Internal
  void handleInternalSignRemove(@NotNull WorldPosition position);

  @Internal
  void handleInternalSignConfigUpdate(@NotNull SignsConfiguration configuration);
}
