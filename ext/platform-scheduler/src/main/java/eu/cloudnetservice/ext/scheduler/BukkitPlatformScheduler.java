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

import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;

/**
 * Bukkit implementation of a platform scheduler.
 *
 * @since 4.0
 */
final class BukkitPlatformScheduler implements PlatformScheduler {

  private static final BukkitScheduler SCHEDULER = Bukkit.getScheduler();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ScheduledPlatformTask globalRunDelayed(
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    long delayTicks
  ) {
    var bukkitTask = SCHEDULER.runTaskLater(plugin, task, delayTicks);
    return new BukkitScheduledPlatformTask(bukkitTask);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ScheduledPlatformTask globalRunAtFixedRate(
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    long initialDelayTicks,
    long periodTicks
  ) {
    var bukkitTask = SCHEDULER.runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
    return new BukkitScheduledPlatformTask(bukkitTask);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ScheduledPlatformTask regionRunDelayed(
    @NonNull Plugin plugin,
    @NonNull World world,
    int chunkX,
    int chunkZ,
    @NonNull Runnable task,
    long delayTicks
  ) {
    return this.globalRunDelayed(plugin, task, delayTicks);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ScheduledPlatformTask regionRunAtFixedRate(
    @NonNull Plugin plugin,
    @NonNull World world,
    int chunkX,
    int chunkZ,
    @NonNull Runnable task,
    long initialDelayTicks,
    long periodTicks
  ) {
    return this.globalRunAtFixedRate(plugin, task, initialDelayTicks, periodTicks);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ScheduledPlatformTask asyncRunNow(@NonNull Plugin plugin, @NonNull Runnable task) {
    var bukkitTask = SCHEDULER.runTaskAsynchronously(plugin, task);
    return new BukkitScheduledPlatformTask(bukkitTask);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ScheduledPlatformTask asyncRunDelayed(
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    long delay,
    @NonNull TimeUnit unit
  ) {
    var bukkitTask = SCHEDULER.runTaskLaterAsynchronously(
      plugin,
      task,
      unit.toMillis(delay) / 50);
    return new BukkitScheduledPlatformTask(bukkitTask);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ScheduledPlatformTask asyncRunAtFixedRate(
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    long initialDelay,
    long period,
    @NonNull TimeUnit unit
  ) {
    var bukkitTask = SCHEDULER.runTaskTimerAsynchronously(
      plugin,
      task,
      unit.toMillis(initialDelay) / 50,
      unit.toMillis(period) / 50);
    return new BukkitScheduledPlatformTask(bukkitTask);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ScheduledPlatformTask entityRunDelayed(
    @NonNull Entity entity,
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    @Nullable Runnable retired,
    long delayTicks
  ) {
    return this.globalRunDelayed(plugin, task, delayTicks);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ScheduledPlatformTask entityRunAtFixedRate(
    @NonNull Entity entity,
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    @Nullable Runnable retired,
    long initialDelayTicks,
    long periodTicks
  ) {
    return this.globalRunAtFixedRate(plugin, task, initialDelayTicks, periodTicks);
  }
}
