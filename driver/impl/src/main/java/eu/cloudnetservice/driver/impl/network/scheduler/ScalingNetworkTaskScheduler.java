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
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import lombok.NonNull;

/**
 * An implementation of a network task scheduler that slowly starts more threads depending on the count of tasks that
 * couldn't be scheduled into the core executor service. If more and more tasks are starting to come in and the core
 * thread pool is not able to keep up, the pace of starting new threads increases. Initially the time between thread
 * starts is 2.5 seconds, and it can go all the way down to 100ms if needed (starting at around 160 tasks waiting to be
 * scheduled).
 *
 * @since 4.0
 */
public final class ScalingNetworkTaskScheduler implements NetworkTaskScheduler {

  private static final int BASE_QUEUE_TIMEOUT = 150;
  private static final int MINIMUM_QUEUE_TIMEOUT = 50;
  private static final double QUEUE_TIMOUT_DECAY_FACTOR = 0.95;

  private final AtomicBoolean active;
  private final ThreadPoolExecutor coreExecutor;
  private final ThreadPoolExecutor fallbackExecutor;
  private final TaskSchedulingAction taskScheduler;

  /**
   * Constructs a new scaling task executor.
   *
   * @param threadFactory       the thread factory to use when the underlying executors create new threads.
   * @param maximumCorePoolSize the maximum threads that the core executor can use.
   * @throws NullPointerException     if the given thread factory is null.
   * @throws IllegalArgumentException if the given maximum pool size is smaller than 0.
   */
  public ScalingNetworkTaskScheduler(@NonNull ThreadFactory threadFactory, int maximumCorePoolSize) {
    this.active = new AtomicBoolean(true);

    // use a thread pool with an infinite possible amount of threads as a fallback, but only
    // keep these threads alive for a few seconds. this only the last-resort executor and shouldn't
    // have a lot of resources allocated for no reason. note that there is no rejection handler
    // set, this executor can technically not reject tasks because it can spawn an infinite
    // amount of threads
    this.fallbackExecutor = new ThreadPoolExecutor(
      0,
      Integer.MAX_VALUE,
      5L,
      TimeUnit.SECONDS,
      new SynchronousQueue<>(),
      threadFactory);

    // construct the calculator for timeouts when adding tasks into the core executor,
    // which depends on the count of unscheduled (more unscheduled tasks = smaller timeout)
    LinkedBlockingQueue<Runnable> unscheduledTaskQueue = new LinkedBlockingQueue<>();
    LongSupplier timeoutSupplier = () -> {
      var scheduledTasks = unscheduledTaskQueue.size();
      var timeout = BASE_QUEUE_TIMEOUT * Math.pow(QUEUE_TIMOUT_DECAY_FACTOR, scheduledTasks);
      return (long) Math.clamp(timeout, MINIMUM_QUEUE_TIMEOUT, BASE_QUEUE_TIMEOUT);
    };

    // construct the core executor for incoming packets, which should be used primarily
    // for scheduling actions. if this scheduler runs out of available threads (for example
    // if they are all blocked) after a short timeout the tasks are scheduled into a fallback
    // scheduler which has no size limits and takes over the work to not possibly run into
    // an issue with too many blocked threads
    TimedSynchronousQueue<Runnable> workQueue = new TimedSynchronousQueue<>(timeoutSupplier);
    var fallbackDelegatingRejectionHandler = new FallbackRejectionHandler(this.fallbackExecutor);
    this.coreExecutor = new ThreadPoolExecutor(
      maximumCorePoolSize / 2,
      maximumCorePoolSize,
      30L,
      TimeUnit.SECONDS,
      workQueue,
      threadFactory,
      fallbackDelegatingRejectionHandler);
    workQueue.parentExecutor(this.coreExecutor); // allows the queue to make some optimizations

    // construct and start the actual task scheduling action, uses a virtual
    // thread here as the action blocks most of the time waiting for tasks
    // to be executed
    this.taskScheduler = new TaskSchedulingAction(this.coreExecutor, unscheduledTaskQueue);
    Thread.ofVirtual().name("NetworkTaskScheduler").start(this.taskScheduler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void shutdown() {
    if (this.active.compareAndSet(true, false)) {
      this.coreExecutor.shutdownNow();
      this.fallbackExecutor.shutdownNow();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Runnable command) {
    if (!this.active.get()) {
      // the underlying scheduler were also shut down, there is no point
      // in even trying to schedule a new task in them
      throw new RejectedExecutionException("scheduler was shut down");
    }

    // scheduling directly into the core executor would be a blocking operation
    // so we use this "man-in-the-middle" action that does the scheduling into
    // the core executor without blocking the caller of this method
    this.taskScheduler.scheduleTask(command);
  }
}
