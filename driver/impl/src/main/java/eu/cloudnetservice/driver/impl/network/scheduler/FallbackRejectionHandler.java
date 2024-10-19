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

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.NonNull;

/**
 * A rejected execution handler that starts the rejected task in the provided fallback executor.
 *
 * @param fallbackExecutor the fallback executor to start rejected starts in.
 * @since 4.0
 */
record FallbackRejectionHandler(@NonNull ThreadPoolExecutor fallbackExecutor) implements RejectedExecutionHandler {

  /**
   * Schedules the given rejected task in the fallback executor.
   *
   * @param task              the task requested to be executed.
   * @param rejectingExecutor the executor that failed to schedule the task.
   * @throws NullPointerException if the given task or rejecting executor is null.
   */
  @Override
  public void rejectedExecution(@NonNull Runnable task, @NonNull ThreadPoolExecutor rejectingExecutor) {
    if (!rejectingExecutor.isShutdown() && !this.fallbackExecutor.isShutdown()) {
      try {
        this.fallbackExecutor.execute(task);
      } catch (RejectedExecutionException _) {
        // ignore this, must be due to a concurrent shutdown call
      }
    }
  }
}
