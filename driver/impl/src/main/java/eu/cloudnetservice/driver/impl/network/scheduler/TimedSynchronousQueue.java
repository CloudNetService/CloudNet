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

import com.google.common.base.Preconditions;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import lombok.NonNull;

/**
 * An implementation of a synchronous queue that always offers tasks to itself with a timeout instead of returning
 * instantly if that is not possible. The queue also makes some more assumptions when the parent executor service is
 * provided to achieve the maximum scaling of the parent thread pool.
 *
 * @param <E> the type of elements held in this queue
 * @since 4.0
 */
final class TimedSynchronousQueue<E> extends SynchronousQueue<E> {

  private final LongSupplier timeoutSupplier;
  private ThreadPoolExecutor parentExecutor;

  /**
   * Constructs a new instance of this queue using the given timeout supplier.
   *
   * @param timeoutSupplier the supplier for timeouts used when actually needing to queue elements.
   * @throws NullPointerException if the given timeout supplier is null.
   */
  public TimedSynchronousQueue(@NonNull LongSupplier timeoutSupplier) {
    super(true); // let waiting threads contend in FIFO order
    this.timeoutSupplier = timeoutSupplier;
  }

  /**
   * Sets the parent thread pool executor that is associated with the queue unless another thread pool executor is
   * already associated with it.
   *
   * @param parentExecutor the parent executor to use for this queue.
   * @throws NullPointerException  if the given parent executor is null.
   * @throws IllegalStateException if a parent executor was already set.
   */
  public void parentExecutor(@NonNull ThreadPoolExecutor parentExecutor) {
    Preconditions.checkState(this.parentExecutor == null, "parent executor already set");
    this.parentExecutor = parentExecutor;
  }

  /**
   * Tries to offer the given element into this queue with a timeout provided by the owner of this queue. If the queue
   * has information about the associated thread pool this method makes some additional considerations to ensure scaling
   * the thread pool to its maximum capacity first.
   *
   * @param element the element to add into this queue.
   * @return true if the element was added into this queue, false otherwise.
   * @throws NullPointerException if the given element is null.
   */
  @Override
  public boolean offer(@NonNull E element) {
    var executor = this.parentExecutor;
    if (executor != null) {
      // reject immediately if the parent executor is already shut down, no need to try
      // to queue it as there won't be any thread to pick up the given task anyway
      if (executor.isShutdown()) {
        return false;
      }

      // check if there are idling threads in the thread pool at the moment. if that is the case
      // try to enqueue the element for 50ms, which should be more than enough for one of the idling
      // workers to wake up and poll the task from this queue.
      // note that the returned active count is an approximation, therefore it could be that there are
      // no free threads anymore when the actual offer call happens, hence the timeout
      if (executor.getActiveCount() < executor.getPoolSize()) {
        var polledByExecutor = this.offerSafe(element, 50);
        if (polledByExecutor) {
          return true;
        }
      }

      // check if the threads of the executor are already maxed out. TPE prefers to queue
      // tasks instead of spawning a new thread for them, this basically forces the executor
      // to go ahead and spawn a new one as we aren't at the maximum pool size yet
      if (executor.getPoolSize() < executor.getMaximumPoolSize()) {
        return false;
      }
    }

    // either no information is available about the thread pool or the thread pool is completely
    // busy at the moment. try to queue the task with a timeout, if this doesn't work the task
    // will be rejected which we can handle somewhere else
    var offerTimeout = this.timeoutSupplier.getAsLong();
    return this.offerSafe(element, offerTimeout);
  }

  /**
   * Tries to offer the given element into this queue with the given timeout millis.
   *
   * @param element       the element to offer into this queue.
   * @param timeoutMillis the timeout in milliseconds of the offer operation.
   * @return true if some other thread polled the element within the timeout millis, false otherwise.
   * @throws NullPointerException if the given element is null.
   */
  private boolean offerSafe(@NonNull E element, long timeoutMillis) {
    try {
      return super.offer(element, timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt(); // reset interrupted state
      return false;
    }
  }
}
