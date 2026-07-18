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

package eu.cloudnetservice.modules.smart;

import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Represents the management interface for smart service task configurations.
 *
 * @since 4.0
 */
public interface SmartServiceManagement {

  /**
   * Gets an unmodifiable view of all smart service task configurations mapped by their target task name.
   *
   * @return an unmodifiable view of all smart service task configurations.
   */
  @NonNull
  @UnmodifiableView
  Map<String, SmartServiceTaskConfig> configurations();

  /**
   * Gets the smart service task entry with the given task name as {@link SmartServiceTaskConfig#targetTask()}.
   *
   * @param taskName the targetTask name of the smart service task configuration to get.
   * @return the smart service task configuration or null if not present.
   * @throws NullPointerException if the given task name is null.
   */
  @Nullable SmartServiceTaskConfig smartServiceTaskConfig(@NonNull String taskName);

  /**
   * Adds or updates the given smart service task configuration. If there is already a configuration for the same task
   * name, the old one is replaced.
   * <p>
   * The configuration is synced to all other nodes in the cluster.
   *
   * @param config the smart service task configuration to add.
   * @throws NullPointerException if the given configuration is null.
   */
  void addSmartServiceTaskConfig(@NonNull SmartServiceTaskConfig config);

  /**
   * Removes the smart service task configuration based on the given task name.
   * <p>
   * The configuration is removed from all other nodes in the cluster.
   *
   * @param taskName the targetTask name of the smart service task configuration to remove.
   * @throws NullPointerException if the given task name is null.
   */
  void removeSmartServiceTaskConfig(@NonNull String taskName);
}
