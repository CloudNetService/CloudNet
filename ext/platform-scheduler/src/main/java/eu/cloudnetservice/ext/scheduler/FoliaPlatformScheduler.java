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

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * Folia implementation of a platform scheduler.
 *
 * @since 4.0
 */
final class FoliaPlatformScheduler implements BukkitPlatformScheduler {

  private static final AsyncScheduler ASYNC_SCHEDULER = Bukkit.getAsyncScheduler();
  private static final RegionScheduler REGION_SCHEDULER = Bukkit.getRegionScheduler();
  private static final GlobalRegionScheduler GLOBAL_REGION_SCHEDULER = Bukkit.getGlobalRegionScheduler();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ScheduledPlatformTask globalRunDelayed(
    @NonNull Plugin plugin,
    @NonNull Runnable task,
    long delayTicks
  ) {
    var scheduledTask = GLOBAL_REGION_SCHEDULER.runDelayed(plugin, _ -> task.run(), delayTicks);
    return new TaskWrapper(scheduledTask);
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
    var scheduledTask = GLOBAL_REGION_SCHEDULER.runAtFixedRate(plugin, _ -> task.run(), initialDelayTicks, periodTicks);
    return new TaskWrapper(scheduledTask);
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
    var scheduledTask = REGION_SCHEDULER.runDelayed(plugin, world, chunkX, chunkZ, _ -> task.run(), delayTicks);
    return new TaskWrapper(scheduledTask);
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
    var scheduledTask = REGION_SCHEDULER.runAtFixedRate(
      plugin,
      world,
      chunkX,
      chunkZ,
      _ -> task.run(),
      initialDelayTicks,
      periodTicks);
    return new TaskWrapper(scheduledTask);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ScheduledPlatformTask asyncRunNow(
    @NonNull Plugin plugin,
    @NonNull Runnable task
  ) {
    var scheduledTask = ASYNC_SCHEDULER.runNow(plugin, _ -> task.run());
    return new TaskWrapper(scheduledTask);
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
    var scheduledTask = ASYNC_SCHEDULER.runDelayed(plugin, _ -> task.run(), delay, unit);
    return new TaskWrapper(scheduledTask);
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
    var scheduledTask = ASYNC_SCHEDULER.runAtFixedRate(plugin, _ -> task.run(), initialDelay, period, unit);
    return new TaskWrapper(scheduledTask);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ScheduledPlatformTask entityRunDelayed(
    @NonNull Plugin plugin,
    @NonNull Entity entity,
    @NonNull Runnable task,
    long delayTicks
  ) {
    var scheduler = entity.getScheduler();
    var scheduledTask = scheduler.runDelayed(plugin, _ -> task.run(), task, delayTicks);
    return scheduledTask == null ? null : new TaskWrapper(scheduledTask);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ScheduledPlatformTask entityRunAtFixedRate(
    @NonNull Plugin plugin,
    @NonNull Entity entity,
    @NonNull Runnable task,
    long initialDelayTicks,
    long periodTicks
  ) {
    var scheduler = entity.getScheduler();
    var scheduledTask = scheduler.runAtFixedRate(plugin, _ -> task.run(), task, initialDelayTicks, periodTicks);
    return scheduledTask == null ? null : new TaskWrapper(scheduledTask);
  }

  /**
   * Wrapper for a task scheduled on a folia task scheduler.
   *
   * @param task the wrapped task.
   */
  private record TaskWrapper(@NonNull ScheduledTask task) implements ScheduledPlatformTask {

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {
      this.task.cancel();
    }
  }
}
