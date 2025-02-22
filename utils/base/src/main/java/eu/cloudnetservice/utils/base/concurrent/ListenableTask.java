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

package eu.cloudnetservice.utils.base.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;

/**
 * The listenable task is a task that is allowed to be executed over and over again. The task wraps a callable and calls
 * it on each run of the task. Once the result of the task is completed using {@code task.run(true)} the task cannot run
 * anymore, and it switches into the done state.
 *
 * @param <V> the generic type of the task.
 * @since 4.0
 */
public class ListenableTask<V> extends CompletableFuture<V> {

  protected static final int STATE_NEW = 0;
  protected static final int STATE_RUN = 1;
  protected static final int STATE_DONE = 2;

  private final Callable<V> callable;
  private final AtomicInteger state = new AtomicInteger(STATE_NEW);

  /**
   * Constructs a new listenable task with the given callable
   *
   * @param callable the callable to call on each run.
   * @throws NullPointerException if the given callable is null.
   */
  public ListenableTask(@NonNull Callable<V> callable) {
    this.callable = callable;
  }

  /**
   * Runs the callable provided to this listenable task. This task supports running it over and over again until the
   * result it set and the task completed with that result.
   *
   * @param setResult whether the result of the callable should be set as result of this task.
   */
  public void run(boolean setResult) {
    // check if we can run the task now
    if (this.state.compareAndSet(STATE_NEW, STATE_RUN)) {
      try {
        // execute the callable
        var result = this.callable.call();
        if (setResult) {
          // complete and mark as done to never run again
          this.complete(result);
          this.state.setRelease(STATE_DONE);
        } else {
          // reset for further runs
          this.state.set(STATE_NEW);
        }
      } catch (Throwable throwable) {
        // complete exceptionally and mark as done to never run again
        this.completeExceptionally(throwable);
        this.state.setRelease(STATE_DONE);
      }
    }
  }
}
