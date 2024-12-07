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

package eu.cloudnetservice.modules.signs;

import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import java.util.Collection;
import lombok.NonNull;
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
  @Nullable Sign signAt(@NonNull WorldPosition position);

  /**
   * Creates a new sign.
   *
   * @param sign the sign to create
   */
  void createSign(@NonNull Sign sign);

  /**
   * Deletes the specified sign.
   *
   * @param sign the sign to delete.
   */
  void deleteSign(@NonNull Sign sign);

  /**
   * Deletes the sign at the given position.
   *
   * @param position the position of the sign to delete.
   */
  void deleteSign(@NonNull WorldPosition position);

  /**
   * Deletes all signs of the specified group.
   *
   * @param group the group to delete the signs of
   * @return the amount of deleted signs
   */
  int deleteAllSigns(@NonNull String group);

  /**
   * Deletes all signs of the specified group.
   *
   * @param group        the group to delete the signs of
   * @param templatePath the template path of the signs to delete
   * @return the amount of deleted signs
   */
  int deleteAllSigns(@NonNull String group, @Nullable String templatePath);

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
  @NonNull
  Collection<Sign> signs();

  /**
   * Get all signs of the specified groups.
   *
   * @param groups the groups the signs are created on
   * @return all signs that are created on the given groups
   */
  @NonNull
  Collection<Sign> signs(@NonNull Collection<String> groups);

  /**
   * Get the current sign configuration.
   *
   * @return the current sign configuration
   */
  @NonNull
  SignsConfiguration signsConfiguration();

  /**
   * Sets the sign configuration and updates it to all connected components.
   *
   * @param configuration the new signs configuration.
   */
  void signsConfiguration(@NonNull SignsConfiguration configuration);
}
