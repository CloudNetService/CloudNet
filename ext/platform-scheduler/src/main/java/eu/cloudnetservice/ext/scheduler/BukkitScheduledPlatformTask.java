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

package eu.cloudnetservice.ext.scheduler;

import lombok.NonNull;
import org.bukkit.scheduler.BukkitTask;

/**
 * Bukkit implementation of a scheduled platform task.
 *
 * @since 4.0
 */
final class BukkitScheduledPlatformTask implements ScheduledPlatformTask {

  private final BukkitTask bukkitTask;

  /**
   * Creates a new scheduled platform task instance for a given bukkit task.
   *
   * @param bukkitTask the bukkit task to wrap.
   * @throws NullPointerException if bukkitTask is null.
   */
  BukkitScheduledPlatformTask(@NonNull BukkitTask bukkitTask) {
    this.bukkitTask = bukkitTask;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancel() {
    this.bukkitTask.cancel();
  }
}
