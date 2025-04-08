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

package eu.cloudnetservice.driver.impl.network.scheduler;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.NonNull;

/**
 * The task scheduler runnable that is responsible for scheduling incoming tasks into the target executor service. Due
 * to the fact that the scheduling action could be blocking, this action should be scheduled in a separate thread.
 *
 * @since 4.0
 */
final class TaskSchedulingAction implements Runnable {

  private final ThreadPoolExecutor targetExecutor;
  private final LinkedBlockingQueue<Runnable> unscheduledTasks;

  /**
   * Constructs a new task scheduling action.
   *
   * @param targetExecutor       the executor to execute unscheduled tasks.
   * @param unscheduledTaskQueue the queue for unscheduled tasks.
   * @throws NullPointerException if the given target executor or unscheduled task queue is null.
   */
  public TaskSchedulingAction(
    @NonNull ThreadPoolExecutor targetExecutor,
    @NonNull LinkedBlockingQueue<Runnable> unscheduledTaskQueue
  ) {
    this.targetExecutor = targetExecutor;
    this.unscheduledTasks = unscheduledTaskQueue;
  }

  /**
   * Schedules tasks that were added to the unscheduled tasks queue into the target executor. This operation blocks
   * until a task is available in the tasks queue.
   */
  @Override
  public void run() {
    while (true) {
      try {
        var nextTask = this.unscheduledTasks.take();
        if (this.targetExecutor.isShutdown()) {
          // the shutdown call on the target executor might happen
          // while we're waiting for an unscheduled task to become
          // available, therefore we need to check after to prevent
          // scheduling in an executor that was shut down already
          break;
        }

        // this call might block for a bit, depending on the queue used by the
        // target executor (therefore we cannot call it directly on the caller)
        this.targetExecutor.execute(nextTask);
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt(); // reset interrupted state
        break;
      } catch (RejectedExecutionException _) {
        // the executor rejected the execution, this should
        // usually be handled otherwise... but just to be sure
        // that this occurrence wouldn't blow up this scheduler
      }
    }
  }

  /**
   * Schedules the given task for execution.
   *
   * @param task the task to execute.
   * @throws NullPointerException if the given task is null.
   */
  public void scheduleTask(@NonNull Runnable task) {
    this.unscheduledTasks.offer(task);
  }
}
