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
import org.jetbrains.annotations.Nullable;

/**
 * The platform scheduler api allows scheduling tasks independent of the underlying platform.
 *
 * @param <P> the type of the plugin instances used by the platform.
 * @param <W> the type that represents worlds on the platform.
 * @param <E> the type that represents entities on the platform.
 * @since 4.0
 */
public interface PlatformScheduler<P, W, E> {

  /**
   * Get the current selected platform scheduler instance.
   *
   * @return the current selected platform scheduler instance.
   * @throws UnsupportedOperationException if no platform scheduler implementation is present for the platform.
   */
  static @NonNull PlatformScheduler<?, ?, ?> scheduler() {
    return PlatformSchedulerHolder.HOLDER.get();
  }

  /**
   * Schedules a task to be run on the global main thread during the next server tick.
   *
   * @param plugin the plugin owning the task.
   * @param task   the task to run.
   * @return the scheduled task.
   * @throws NullPointerException if the given plugin or task is null.
   *
   */
  default @NonNull ScheduledPlatformTask globalRun(@NonNull P plugin, @NonNull Runnable task) {
    return this.globalRunDelayed(plugin, task, 1);
  }

  /**
   * Schedules a task to be run on the global main thread after the given delay in ticks.
   *
   * @param plugin     the plugin owning the task.
   * @param task       the task to run.
   * @param delayTicks the delay in ticks before the task is run. Must be greater than 0.
   * @return the scheduled task.
   * @throws NullPointerException if the given plugin or task is null.
   */
  @NonNull
  ScheduledPlatformTask globalRunDelayed(@NonNull P plugin, @NonNull Runnable task, long delayTicks);

  /**
   * Schedules a task to be run on the global main thread at a fixed rate.
   *
   * @param plugin            the plugin owning the task.
   * @param task              the task to run.
   * @param initialDelayTicks the initial delay in ticks before the task is run the first time. Must be greater than 0.
   * @param periodTicks       the period in ticks between subsequent executions of the task. Must be greater than 0.
   * @return the scheduled task.
   * @throws NullPointerException if the given plugin or task is null.
   */
  @NonNull
  ScheduledPlatformTask globalRunAtFixedRate(
    @NonNull P plugin,
    @NonNull Runnable task,
    long initialDelayTicks,
    long periodTicks);

  /**
   * Schedules a task to be run on the next tick on the scheduler responsible for region that contains the given chunk
   * coordinates. On platforms that don't have region scheduling, this method runs on the main thread.
   *
   * @param plugin the plugin owning the task.
   * @param world  the world containing the region.
   * @param chunkX the chunk x coordinate defining the region.
   * @param chunkZ the chunk z coordinate defining the region.
   * @param task   the task to run.
   * @return the scheduled task.
   * @throws NullPointerException if the given plugin, world or task is null.
   */
  default @NonNull ScheduledPlatformTask regionRun(
    @NonNull P plugin,
    @NonNull W world,
    int chunkX,
    int chunkZ,
    @NonNull Runnable task
  ) {
    return this.regionRunDelayed(plugin, world, chunkX, chunkZ, task, 1);
  }

  /**
   * Schedules a task to be run after the given delay in ticks on the scheduler responsible for region that contains the
   * given chunk coordinates. On platforms that don't have region scheduling, this method runs on the main thread.
   *
   * @param plugin     the plugin owning the task.
   * @param world      the world containing the region.
   * @param chunkX     the chunk x coordinate defining the region.
   * @param chunkZ     the chunk z coordinate defining the region.
   * @param task       the task to run.
   * @param delayTicks the delay in ticks before the task is run. Must be greater than 0.
   * @return the scheduled task.
   * @throws NullPointerException if the given plugin, world or task is null.
   */
  @NonNull
  ScheduledPlatformTask regionRunDelayed(
    @NonNull P plugin,
    @NonNull W world,
    int chunkX,
    int chunkZ,
    @NonNull Runnable task,
    long delayTicks);

  /**
   * Schedules a task to be run at a fixed rate on the scheduler responsible for region that contains the given chunk
   * coordinates. On platforms that don't have region scheduling, this method runs on the main thread.
   *
   * @param plugin            the plugin owning the task.
   * @param world             the world containing the region.
   * @param chunkX            the chunk x coordinate defining the region.
   * @param chunkZ            the chunk z coordinate defining the region.
   * @param task              the task to run.
   * @param initialDelayTicks the initial delay in ticks before the task is run the first time. Must be greater than 0.
   * @param periodTicks       the period in ticks between subsequent executions of the task. Must be greater than 0.
   * @return the scheduled task.
   * @throws NullPointerException if the given plugin, world or task is null.
   */
  @NonNull
  ScheduledPlatformTask regionRunAtFixedRate(
    @NonNull P plugin,
    @NonNull W world,
    int chunkX,
    int chunkZ,
    @NonNull Runnable task,
    long initialDelayTicks,
    long periodTicks);

  /**
   * Schedules a task to be run asynchronously as soon as possible.
   *
   * @param plugin the plugin owning the task.
   * @param task   the task to run.
   * @return the scheduled task.
   * @throws NullPointerException if the given plugin or task is null.
   */
  @NonNull
  ScheduledPlatformTask asyncRunNow(@NonNull P plugin, @NonNull Runnable task);

  /**
   * Schedules a task to be run asynchronously after the given delay. On server implementations that don't support exact
   * times for scheduling, the given delay will be rounded to the nearest tick count (half-up).
   *
   * @param plugin the plugin owning the task.
   * @param task   the task to run.
   * @param delay  the delay before the task is run.
   * @param unit   the time unit of the delay.
   * @return the scheduled task.
   * @throws NullPointerException if the given plugin, task or unit is null.
   */
  @NonNull
  ScheduledPlatformTask asyncRunDelayed(
    @NonNull P plugin,
    @NonNull Runnable task,
    long delay,
    @NonNull TimeUnit unit);

  /**
   * Schedules a task to be run asynchronously at a fixed rate. On server implementations that don't support exact times
   * for scheduling, the given initial delay and period will be rounded to the nearest tick count (half-up).
   *
   * @param plugin       the plugin owning the task.
   * @param task         the task to run.
   * @param initialDelay the initial delay before the task is run the first time.
   * @param period       the period between subsequent executions of the task.
   * @param unit         the time unit of the initial delay and period.
   * @return the scheduled task.
   * @throws NullPointerException if the given plugin, task or unit is null.
   */
  @NonNull
  ScheduledPlatformTask asyncRunAtFixedRate(
    @NonNull P plugin,
    @NonNull Runnable task,
    long initialDelay,
    long period,
    @NonNull TimeUnit unit);

  /**
   * Schedules a task to be run on the next tick for the given entity. On platforms that don't have entity scheduling,
   * this method runs on the main thread. Note: the method returns null in case the given entity is already removed at
   * invocation time, if checking of this status is safe or done by the scheduler implementation. The given task runs
   * even if the entity is removed (after the task was registered), it might get canceled by the server implementation
   * if the entity is removed.
   *
   * @param plugin the plugin owning the task.
   * @param entity the entity the task is bound to.
   * @param task   the task to run.
   * @return the scheduled task, possibly {@code null} if the entity is already removed.
   * @throws NullPointerException if the given plugin, entity or task is null.
   */
  default @Nullable ScheduledPlatformTask entityRun(@NonNull P plugin, @NonNull E entity, @NonNull Runnable task) {
    return this.entityRunDelayed(plugin, entity, task, 1);
  }

  /**
   * Schedules a task to be run after the given delay in ticks for the given entity. On platforms that don't have entity
   * scheduling, this method runs on the main thread. Note: the method returns null in case the given entity is already
   * removed at invocation time, if checking of this status is safe or done by the scheduler implementation. The given
   * task runs even if the entity is removed (after the task was registered), it might get canceled by the server
   * implementation if the entity is removed.
   *
   * @param plugin     the plugin owning the task.
   * @param entity     the entity the task is bound to.
   * @param task       the task to run.
   * @param delayTicks the delay in ticks before the task is run. Must be greater than 0.
   * @return the scheduled task, possibly {@code null} if the entity is already removed.
   * @throws NullPointerException if the given plugin, entity or task is null.
   */
  @Nullable
  ScheduledPlatformTask entityRunDelayed(@NonNull P plugin, @NonNull E entity, @NonNull Runnable task, long delayTicks);

  /**
   * Schedules a task to be run at a fixed rate for the given entity. On platforms that don't have entity scheduling,
   * this method runs on the main thread. Note: the method returns null in case the given entity is already removed at
   * invocation time, if checking of this status is safe or done by the scheduler implementation. The given task runs
   * even if the entity is removed (after the task was registered), it might get canceled by the server implementation
   * if the entity is removed.
   *
   * @param plugin            the plugin owning the task.
   * @param entity            the entity the task is bound to.
   * @param task              the task to run.
   * @param initialDelayTicks the initial delay in ticks before the task is run the first time. Must be greater than 0.
   * @param periodTicks       the period in ticks between subsequent executions of the task. Must be greater than 0.
   * @return the scheduled task, possibly {@code null} if the entity is already removed.
   * @throws NullPointerException if the given plugin, entity or task is null.
   */
  @Nullable
  ScheduledPlatformTask entityRunAtFixedRate(
    @NonNull P plugin,
    @NonNull E entity,
    @NonNull Runnable task,
    long initialDelayTicks,
    long periodTicks);
}
