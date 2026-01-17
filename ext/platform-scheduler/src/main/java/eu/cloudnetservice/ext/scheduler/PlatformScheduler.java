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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * The platform scheduler api allows scheduling tasks independent of the underlying platform.
 * <p>
 * Currently supported are Bukkit based schedulers and if Folia is detected the different Folia schedulers.
 *
 * @since 4.0
 */
public interface PlatformScheduler {

  /**
   * Obtains the jvm static scheduler instance based on the underlying platform.
   *
   * @return the platform scheduler instance.
   */
  static @NonNull PlatformScheduler scheduler() {
    return PlatformSchedulerHolder.INSTANCE.get();
  }

  /**
   * Schedules a global task to be run on the next tick. On Folia this task is executed in the global region. On other
   * platforms it is executed on the main thread.
   *
   * @param plugin the plugin owning the task.
   * @param task   the task to run.
   * @return the scheduled task.
   * @throws NullPointerException if the plugin or the task is null.
   *
   */
  default @NonNull ScheduledPlatformTask globalRun(@NonNull Plugin plugin, @NonNull Runnable task) {
    return this.globalRunDelayed(plugin, task, 1);
  }

  /**
   * Schedules a global task to be run after the given delay in ticks. On Folia this task is executed in the global
   * region. On other platforms it is executed on the main thread.
   *
   * @param plugin     the plugin owning the task.
   * @param task       the task to run.
   * @param delayTicks the delay in ticks before the task is run. Must be > 0.
   * @return the scheduled task.
   * @throws NullPointerException     if the plugin or the task is null.
   * @throws IllegalArgumentException if the delay ticks are less than 1.
   */
  @NonNull
  ScheduledPlatformTask globalRunDelayed(@NonNull Plugin plugin, @NonNull Runnable task, long delayTicks);

  /**
   * Schedules a global task to be run at a fixed rate. On Folia this task is executed in the global region. On other
   * platforms it is executed on the main thread.
   *
   * @param plugin            the plugin owning the task.
   * @param task              the task to run.
   * @param initialDelayTicks the initial delay in ticks before the task is run the first time. Must be > 0.
   * @param periodTicks       the period in ticks between subsequent executions of the task. Must be > 0.
   * @return the scheduled task.
   * @throws NullPointerException     if the plugin or the task is null.
   * @throws IllegalArgumentException if the initial delay ticks or period ticks are less than 1
   */
  @NonNull
  ScheduledPlatformTask globalRunAtFixedRate(
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    long initialDelayTicks,
    long periodTicks);

  /**
   * Schedules a task to be run on the next tick. On Folia this task is executed in the region containing the given
   * location. On other platforms it is executed on the main thread like {@link #globalRun(Plugin, Runnable)}.
   *
   * @param plugin   the plugin owning the task.
   * @param location the location defining the region the task is run in.
   * @param task     the task to run.
   * @return the scheduled task.
   * @throws NullPointerException if the plugin, location or the task is null.
   */
  default @NonNull ScheduledPlatformTask regionRun(
    @NonNull Plugin plugin,
    @NonNull Location location,
    @NonNull Runnable task
  ) {
    return this.regionRunDelayed(plugin, location, task, 1);
  }

  /**
   * Schedules a task to be run on the next tick. On Folia this task is executed in the region defined by the given
   * chunk coordinates and world. On other platforms it is executed on the main thread like
   * {@link #globalRun(Plugin, Runnable)}.
   *
   * @param plugin the plugin owning the task.
   * @param world  the world containing the region.
   * @param chunkX the chunk x coordinate defining the region.
   * @param chunkZ the chunk z coordinate defining the region.
   * @param task   the task to run.
   * @return the scheduled task.
   * @throws NullPointerException if the plugin, world or the task is null.
   */
  default @NonNull ScheduledPlatformTask regionRun(
    @NonNull Plugin plugin,
    @NonNull World world,
    int chunkX,
    int chunkZ,
    @NonNull Runnable task
  ) {
    return this.regionRunDelayed(plugin, world, chunkX, chunkZ, task, 1);
  }

  /**
   * Schedules a task to be run after the given delay in ticks. On Folia this task is executed in the region containing
   * the given location. On other platforms it is executed on the main thread like
   * {@link #globalRunDelayed(Plugin, Runnable, long)}.
   *
   * @param plugin     the plugin owning the task.
   * @param location   the location defining the region the task is run in.
   * @param task       the task to run.
   * @param delayTicks the delay in ticks before the task is run. Must be > 0.
   * @return the scheduled task.
   * @throws NullPointerException     if the plugin, location or the task is null.
   * @throws IllegalArgumentException if the delay ticks are less than 1.
   */
  default @NonNull ScheduledPlatformTask regionRunDelayed(
    @NonNull Plugin plugin,
    @NonNull Location location,
    @NonNull Runnable task,
    long delayTicks
  ) {
    return this.regionRunDelayed(
      plugin,
      location.getWorld(),
      location.getBlockX() >> 4,
      location.getBlockZ() >> 4,
      task,
      delayTicks);
  }

  /**
   * Schedules a task to be run after the given delay in ticks. On Folia this task is executed in the region defined by
   * the given chunk coordinates and world. On other platforms it is executed on the main thread like
   * {@link #globalRunDelayed(Plugin, Runnable, long)}.
   *
   * @param plugin     the plugin owning the task.
   * @param world      the world containing the region.
   * @param chunkX     the chunk x coordinate defining the region.
   * @param chunkZ     the chunk z coordinate defining the region.
   * @param task       the task to run.
   * @param delayTicks the delay in ticks before the task is run. Must be > 0.
   * @return the scheduled task.
   * @throws NullPointerException     if the plugin, world or the task is null.
   * @throws IllegalArgumentException if the delay ticks are less than 1.
   */
  @NonNull
  ScheduledPlatformTask regionRunDelayed(
    @NonNull Plugin plugin,
    @NonNull World world,
    int chunkX,
    int chunkZ,
    @NonNull Runnable task,
    long delayTicks);

  /**
   * Schedules a task to be run at a fixed rate. On Folia this task is executed in the region containing the given
   * location. On other platforms it is executed on the main thread like
   * {@link #globalRunAtFixedRate(Plugin, Runnable, long, long)}.
   *
   * @param plugin            the plugin owning the task.
   * @param location          the location defining the region the task is run in.
   * @param task              the task to run.
   * @param initialDelayTicks the initial delay in ticks before the task is run the first time. Must be > 0.
   * @param periodTicks       the period in ticks between subsequent executions of the task. Must be > 0.
   * @return the scheduled task.
   * @throws NullPointerException     if the plugin, location or the task is null.
   * @throws IllegalArgumentException if the initial delay ticks or period ticks are less than 1
   */
  default @NonNull ScheduledPlatformTask regionRunAtFixedRate(
    @NonNull Plugin plugin,
    @NonNull Location location,
    @NonNull Runnable task,
    long initialDelayTicks,
    long periodTicks
  ) {
    return this.regionRunAtFixedRate(
      plugin,
      location.getWorld(),
      location.getBlockX() >> 4,
      location.getBlockZ() >> 4,
      task,
      initialDelayTicks,
      periodTicks);
  }

  /**
   * Schedules a task to be run at a fixed rate. On Folia this task is executed in the region defined by the given chunk
   * coordinates and world. On other platforms it is executed on the main thread like
   * {@link #globalRunAtFixedRate(Plugin, Runnable, long, long)}.
   *
   * @param plugin            the plugin owning the task.
   * @param world             the world containing the region.
   * @param chunkX            the chunk x coordinate defining the region.
   * @param chunkZ            the chunk z coordinate defining the region.
   * @param task              the task to run.
   * @param initialDelayTicks the initial delay in ticks before the task is run the first time. Must be > 0.
   * @param periodTicks       the period in ticks between subsequent executions of the task. Must be > 0.
   * @return the scheduled task.
   * @throws NullPointerException     if the plugin, world or the task is null.
   * @throws IllegalArgumentException if the initial delay ticks or period ticks are less than 1
   */
  @NonNull
  ScheduledPlatformTask regionRunAtFixedRate(
    @NonNull Plugin plugin,
    @NonNull World world,
    int chunkX,
    int chunkZ,
    @NonNull Runnable task,
    long initialDelayTicks,
    long periodTicks);

  /**
   * Schedules a task to be run asynchronously as soon as possible. On both Folia and other platforms this task is run
   * on a separate async thread.
   *
   * @param plugin the plugin owning the task.
   * @param task   the task to run.
   * @return the scheduled task.
   * @throws NullPointerException if the plugin or the task is null.
   */
  @NonNull
  ScheduledPlatformTask asyncRunNow(@NonNull Plugin plugin, @NonNull Runnable task);

  /**
   * Schedules a task to be run asynchronously after the given delay. On both Folia and other platforms this task is run
   * on a separate async thread.
   *
   * @param plugin the plugin owning the task.
   * @param task   the task to run.
   * @param delay  the delay before the task is run.
   * @param unit   the time unit of the delay.
   * @return the scheduled task.
   * @throws NullPointerException if the plugin, task or unit is null.
   */
  @NonNull
  ScheduledPlatformTask asyncRunDelayed(
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    long delay,
    @NonNull TimeUnit unit);

  /**
   * Schedules a task to be run asynchronously at a fixed rate. On both Folia and other platforms this task is run on a
   * separate async thread.
   *
   * @param plugin       the plugin owning the task.
   * @param task         the task to run.
   * @param initialDelay the initial delay before the task is run the first time.
   * @param period       the period between subsequent executions of the task.
   * @param unit         the time unit of the initial delay and period.
   * @return the scheduled task.
   * @throws NullPointerException if the plugin, task or unit is null.
   */
  @NonNull
  ScheduledPlatformTask asyncRunAtFixedRate(
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    long initialDelay,
    long period,
    @NonNull TimeUnit unit);

  /**
   * Schedules a task to be run on the next tick for the given entity. On Folia this task is bound to the entity instead
   * on a region. On other platforms it is executed on the main thread like {@link #globalRun(Plugin, Runnable)}.
   *
   * @param entity  the entity the task is bound to.
   * @param plugin  the plugin owning the task.
   * @param task    the task to run.
   * @param retired the task to run when the entity is removed before the task could run. This will only be called on
   *                Folia.
   * @return the scheduled task. Null if the entity is already removed.
   * @throws NullPointerException if the entity, plugin or the task is null.
   */
  default @Nullable ScheduledPlatformTask entityRun(
    @NonNull Entity entity,
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    @NonNull Runnable retired
  ) {
    return this.entityRunDelayed(entity, plugin, task, retired, 1);
  }

  /**
   * Schedules a task to be run after the given delay in ticks for the given entity. On Folia this task is bound to the
   * entity instead on a region. On other platforms it is executed on the main thread like
   * {@link #globalRunDelayed(Plugin, Runnable, long)}.
   *
   * @param entity     the entity the task is bound to.
   * @param plugin     the plugin owning the task.
   * @param task       the task to run.
   * @param retired    the task to run when the entity is removed before the task could run. This will only be called on
   *                   Folia.
   * @param delayTicks the delay in ticks before the task is run. Must be > 0.
   * @return the scheduled task. Null if the entity is already removed.
   * @throws NullPointerException if the entity, plugin or the task is null.
   */
  @Nullable
  ScheduledPlatformTask entityRunDelayed(
    @NonNull Entity entity,
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    @Nullable Runnable retired,
    long delayTicks);

  /**
   * Schedules a task to be run at a fixed rate for the given entity. On Folia this task is bound to the entity instead
   * on a region. On other platforms it is executed on the main thread like
   * {@link #globalRunAtFixedRate(Plugin, Runnable, long, long)}.
   *
   * @param entity            the entity the task is bound to.
   * @param plugin            the plugin owning the task.
   * @param task              the task to run.
   * @param retired           the task to run when the entity is removed before the task could run. This will only be
   *                          called on Folia.
   * @param initialDelayTicks the initial delay in ticks before the task is run the first time. Must be > 0.
   * @param periodTicks       the period in ticks between subsequent executions of the task. Must be > 0.
   * @return the scheduled task. Null if the entity is already removed.
   * @throws NullPointerException if the entity, plugin or the task is null.
   */
  @Nullable
  ScheduledPlatformTask entityRunAtFixedRate(
    @NonNull Entity entity,
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    @Nullable Runnable retired,
    long initialDelayTicks,
    long periodTicks);
}

