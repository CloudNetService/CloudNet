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

package eu.cloudnetservice.node;

import eu.cloudnetservice.common.concurrent.ListenableTask;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.event.instance.CloudNetTickEvent;
import eu.cloudnetservice.node.event.instance.CloudNetTickServiceStartEvent;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NonNull;

@Singleton
public final class TickLoop {

  public static final int TPS = 10;
  public static final int MILLIS_BETWEEN_TICKS = 1000 / TPS;

  // exposed to the package for internal use
  static final AtomicBoolean RUNNING = new AtomicBoolean(true);

  private static final Logger LOGGER = LogManager.logger(TickLoop.class);

  private final EventManager eventManager;
  private final ServiceTaskProvider taskProvider;
  private final CloudServiceManager serviceManager;
  private final NodeServerProvider nodeServerProvider;
  private final Provider<ShutdownHandler> shutdownHandlerProvider;

  private final AtomicInteger tickPauseRequests = new AtomicInteger();

  private final CloudNetTickEvent tickEvent = new CloudNetTickEvent(this);
  private final CloudNetTickServiceStartEvent serviceTickStartEvent = new CloudNetTickServiceStartEvent(this);

  private final AtomicLong currentTick = new AtomicLong();
  private final Queue<ScheduledTask<?>> processQueue = new ConcurrentLinkedQueue<>();

  @Inject
  public TickLoop(
    @NonNull EventManager eventManager,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull CloudServiceManager serviceManager,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull Provider<ShutdownHandler> shutdownHandlerProvider
  ) {
    this.eventManager = eventManager;
    this.taskProvider = taskProvider;
    this.serviceManager = serviceManager;
    this.nodeServerProvider = nodeServerProvider;
    this.shutdownHandlerProvider = shutdownHandlerProvider;
  }

  public @NonNull Task<Void> runTask(@NonNull Runnable runnable) {
    return this.runTask(() -> {
      runnable.run();
      return null;
    });
  }

  public @NonNull <T> Task<T> runTask(@NonNull Callable<T> callable) {
    var task = new ScheduledTask<>(callable, 0, 1, this.currentTick.get() + 1);
    this.processQueue.offer(task);
    return task;
  }

  public @NonNull Task<Void> runDelayedTask(@NonNull Runnable runnable, long delay, @NonNull TimeUnit timeUnit) {
    return this.runDelayedTask(() -> {
      runnable.run();
      return null;
    }, delay, timeUnit);
  }

  public @NonNull <T> Task<T> runDelayedTask(@NonNull Callable<T> callable, long delay, @NonNull TimeUnit timeUnit) {
    var task = new ScheduledTask<>(
      callable,
      0,
      1,
      this.currentTick.get() + (timeUnit.toMillis(delay) / MILLIS_BETWEEN_TICKS));
    this.processQueue.offer(task);
    return task;
  }

  public @NonNull <T> Task<T> scheduleTask(@NonNull Callable<T> callable, long delay) {
    return this.scheduleTask(callable, delay, -1);
  }

  public @NonNull <T> Task<T> scheduleTask(@NonNull Callable<T> callable, long delay, long maxExecutions) {
    var task = new ScheduledTask<>(
      callable,
      delay,
      maxExecutions,
      this.currentTick.get() + delay);
    this.processQueue.offer(task);
    return task;
  }

  public void pause() {
    this.tickPauseRequests.incrementAndGet();
  }

  public void resume() {
    this.tickPauseRequests.decrementAndGet();
  }

  public long currentTick() {
    return this.currentTick.get();
  }

  public void start() {
    long tick;
    long lastTickLength;
    var lastTick = System.currentTimeMillis();

    while (RUNNING.get()) {
      try {
        // update the current tick we are in
        tick = this.currentTick.incrementAndGet();
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

        // check if ticking is currently disabled
        if (this.tickPauseRequests.get() <= 0) {
          // execute all scheduled tasks for this tick
          for (var task : this.processQueue) {
            if (task.isCancelled() || task.execute(tick)) {
              this.processQueue.remove(task);
            }
          }

          // check if the node is marked for draining
          if (this.nodeServerProvider.localNode().draining()) {
            // check if there are no services on the node
            if (this.serviceManager.localCloudServices().isEmpty()) {
              // stop the node as it's marked for draining
              this.shutdownHandlerProvider.get().shutdown();
              return;
            }
          }

          // check if we should start a service now
          if (this.nodeServerProvider.localNode().head() && tick % TPS == 0) {
            // ensure that there are no idling node servers before we start any service to prevent duplicates
            var idlingNode = this.nodeServerProvider.nodeServers().stream()
              .noneMatch(server -> server.state() == NodeServerState.DISCONNECTED);
            if (idlingNode) {
              this.startService();
              this.eventManager.callEvent(this.serviceTickStartEvent);
            }
          }

          this.eventManager.callEvent(this.tickEvent);
        }
      } catch (Exception exception) {
        LOGGER.severe("Exception while ticking", exception);
      }
    }
  }

  private void startService() {
    for (var task : this.taskProvider.serviceTasks()) {
      if (!task.maintenance()) {
        // get the count of running services
        var runningServiceCount = this.serviceManager.servicesByTask(task.name())
          .stream()
          .filter(taskService -> taskService.lifeCycle() == ServiceLifeCycle.RUNNING)
          .count();
        // check if we need to start a service
        if (task.minServiceCount() > runningServiceCount) {
          this.serviceManager.selectOrCreateService(task).start();
        }
      }
    }
  }

  private static final class ScheduledTask<T> extends ListenableTask<T> {

    /**
     * The number of ticks between each call of this task.
     */
    private final long tickPeriod;
    /**
     * The number of times this task should execute.
     */
    private final long executionTimes;

    /**
     * The counter keeping track of the number of times this task was executed
     */
    private long executionCounter;
    /**
     * The next tick this task is about to execute.
     */
    private long nextScheduledTick;

    public ScheduledTask(@NonNull Callable<T> callable, long tickPeriod, long executionTimes, long nextScheduledTick) {
      super(callable);

      this.tickPeriod = tickPeriod;
      this.executionTimes = executionTimes;
      this.nextScheduledTick = nextScheduledTick;
    }

    /**
     * Executes this task and resets the future to prepare for the next execution.
     *
     * @param currentTick the current tick number.
     * @return true if this task terminated and should be unregistered after the execution, false otherwise.
     */
    private boolean execute(long currentTick) {
      // check if the task is scheduled to run in this tick
      if (this.nextScheduledTick <= currentTick) {
        // check if the execution limit is reached
        if (this.executionTimes != -1 && ++this.executionCounter >= this.executionTimes) {
          // execute the task one last time - no reset
          super.run(true);
          return true;
        }
        // execute the task and reset
        super.run(false);
        // set the next scheduled tick
        this.nextScheduledTick = currentTick + this.tickPeriod;
      }
      // runs again or later
      return false;
    }
  }
}
