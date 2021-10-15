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

package de.dytanic.cloudnet;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.instance.CloudNetTickEvent;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;

public final class CloudNetTick {

  public static final int TPS = 10;
  public static final int MILLIS_BETWEEN_TICKS = 1000 / TPS;

  private static final Logger LOGGER = LogManager.getLogger(CloudNetTick.class);

  private final CloudNet cloudNet;
  private final AtomicLong currentTick = new AtomicLong();
  private final Queue<ScheduledTask<?>> processQueue = new ConcurrentLinkedQueue<>();

  public CloudNetTick(CloudNet cloudNet) {
    this.cloudNet = cloudNet;
  }

  public @NotNull ITask<Void> runTask(@NotNull Runnable runnable) {
    return this.runTask(() -> {
      runnable.run();
      return null;
    });
  }

  public @NotNull <T> ITask<T> runTask(@NotNull Callable<T> callable) {
    ScheduledTask<T> task = new ScheduledTask<>(callable, this.currentTick.get() + 1);
    this.processQueue.offer(task);
    return task;
  }

  public @NotNull ITask<Void> runDelayedTask(@NotNull Runnable runnable, long delay, @NotNull TimeUnit timeUnit) {
    return this.runDelayedTask(() -> {
      runnable.run();
      return null;
    }, delay, timeUnit);
  }

  public @NotNull <T> ITask<T> runDelayedTask(@NotNull Callable<T> callable, long delay, @NotNull TimeUnit timeUnit) {
    ScheduledTask<T> task = new ScheduledTask<>(
      callable,
      this.currentTick.get() + (timeUnit.toMillis(delay) / MILLIS_BETWEEN_TICKS));
    this.processQueue.offer(task);
    return task;
  }

  public void start() {
    long tick;
    long lastTickLength;
    long lastTick = System.currentTimeMillis();

    while (this.cloudNet.isRunning()) {
      try {
        // update the current tick we are in
        tick = this.currentTick.getAndIncrement();
        // calculate oversleep time
        lastTickLength = System.currentTimeMillis() - lastTick;
        if (lastTickLength < MILLIS_BETWEEN_TICKS) {
          try {
            //noinspection BusyWait
            Thread.sleep(MILLIS_BETWEEN_TICKS - lastTickLength);
          } catch (Exception exception) {
            LOGGER.severe("Exception while oversleeping tick time", exception);
          }
        }

        // update the last tick time
        lastTick = System.currentTimeMillis();

        // execute all scheduled tasks for this tick
        for (ScheduledTask<?> task : this.processQueue) {
          if (task.scheduledTick <= tick) {
            task.call();
            this.processQueue.remove(task);
          }
        }

        // check if we should start a service now
        if (this.cloudNet.getClusterNodeServerProvider().getSelfNode().isHeadNode() && tick % TPS == 0) {
          this.startService();
        }

        this.cloudNet.getEventManager().callEvent(new CloudNetTickEvent());
      } catch (Exception exception) {
        LOGGER.severe("Exception while ticking", exception);
      }
    }
  }

  private void startService() {
    for (ServiceTask task : this.cloudNet.getServiceTaskProvider().getPermanentServiceTasks()) {
      if (task.canStartServices()) {
        // get the count of running services
        long runningServiceCount = this.cloudNet.getCloudServiceProvider().getCloudServices(task.getName()).stream()
          .filter(taskService -> taskService.getLifeCycle() == ServiceLifeCycle.RUNNING)
          .count();
        // check if we need to start a service
        if (task.getMinServiceCount() > runningServiceCount) {
          this.cloudNet.getCloudServiceProvider().startService(task).start();
        }
      }
    }
  }

  private static final class ScheduledTask<T> extends ListenableTask<T> {

    private final long scheduledTick;

    public ScheduledTask(@NotNull Callable<T> callable, long scheduledTick) {
      super(callable);
      this.scheduledTick = scheduledTick;
    }
  }
}
