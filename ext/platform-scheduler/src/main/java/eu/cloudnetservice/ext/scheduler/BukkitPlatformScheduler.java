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
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Base type for all scheduler implementations that target a platform that implements the bukkit api.
 *
 * @since 4.0
 */
public sealed interface BukkitPlatformScheduler
  extends PlatformScheduler<Plugin, World, Entity>
  permits RawBukkitPlatformScheduler, FoliaPlatformScheduler {

  /**
   * Get the selected bukkit platform scheduler instance.
   *
   * @return the selected bukkit platform scheduler instance.
   * @throws UnsupportedOperationException if the current platform is not a bukkit platform.
   */
  static @NonNull BukkitPlatformScheduler bukkitScheduler() {
    var scheduler = PlatformScheduler.scheduler();
    if (!(scheduler instanceof BukkitPlatformScheduler bukkitPlatformScheduler)) {
      throw new UnsupportedOperationException("Selected platform scheduler impl is not a bukkit platform scheduler");
    }

    return bukkitPlatformScheduler;
  }
}
