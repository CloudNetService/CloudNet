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

package de.dytanic.cloudnet.common.concurrent;

import java.util.Collection;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@Deprecated
@ApiStatus.ScheduledForRemoval
public class DefaultTaskScheduler implements ITaskScheduler {

  protected static final long DEFAULT_THREAD_LIFE_MILLIS = 60000;
  protected static final long DEFAULT_THREAD_PAUSE_MILLIS = 5;

  protected static final AtomicInteger GROUP_COUNT = new AtomicInteger();

  protected final Queue<IWorkableThread> workers = new ConcurrentLinkedQueue<>();
  protected final Deque<IScheduledTask<?>> taskEntries = new ConcurrentLinkedDeque<>();

  protected final AtomicLong threadCount = new AtomicLong();
  protected final ThreadGroup threadGroup = new ThreadGroup("DefaultTaskScheduler-" + GROUP_COUNT.incrementAndGet());

  protected volatile int maxThreadSize;
  protected volatile long threadLifeMillis;
  protected volatile long threadPauseDelayMillis;

  public DefaultTaskScheduler() {
    this(Runtime.getRuntime().availableProcessors() * 2, DEFAULT_THREAD_LIFE_MILLIS, DEFAULT_THREAD_PAUSE_MILLIS);
  }

  public DefaultTaskScheduler(int maxThreadSize) {
    this(maxThreadSize, DEFAULT_THREAD_LIFE_MILLIS, DEFAULT_THREAD_PAUSE_MILLIS);
  }

  public DefaultTaskScheduler(int maxThreadSize, long threadLifeMillis, long threadPauseDelayMillis) {
    this.maxThreadSize = maxThreadSize <= 0 ? Runtime.getRuntime().availableProcessors() : maxThreadSize;
    this.threadLifeMillis = threadLifeMillis;
    this.threadPauseDelayMillis = threadPauseDelayMillis;
  }

  public static long getDefaultThreadLifeMillis() {
    return DEFAULT_THREAD_LIFE_MILLIS;
  }

  public static long getDefaultThreadPauseMillis() {
    return DEFAULT_THREAD_PAUSE_MILLIS;
  }

  public static AtomicInteger getGroupCount() {
    return GROUP_COUNT;
  }

  public Deque<IScheduledTask<?>> getTaskEntries() {
    return this.taskEntries;
  }

  public ThreadGroup getThreadGroup() {
    return this.threadGroup;
  }

  public AtomicLong getThreadCount() {
    return this.threadCount;
  }

  public int getMaxThreadSize() {
    return this.maxThreadSize;
  }

  public void setMaxThreadSize(int maxThreadSize) {
    this.maxThreadSize = maxThreadSize;
  }

  public long getThreadLifeMillis() {
    return this.threadLifeMillis;
  }

  public void setThreadLifeMillis(long threadLifeMillis) {
    this.threadLifeMillis = threadLifeMillis;
  }

  public long getThreadPauseDelayMillis() {
    return this.threadPauseDelayMillis;
  }

  public void setThreadPauseDelayMillis(long threadPauseDelayMillis) {
    this.threadPauseDelayMillis = threadPauseDelayMillis;
  }

  @Override
  public int getCurrentWorkerCount() {
    return this.workers.size();
  }

  @Override
  public IWorkableThread createWorker() {
    return new Worker();
  }

  @Override
  public IWorkableThread hasFreeWorker() {
    for (IWorkableThread workableThread : this.workers) {
      if (workableThread.isEmpty()) {
        return workableThread;
      }
    }

    return null;
  }

  @Override
  public Collection<IWorkableThread> getWorkers() {
    return this.workers;
  }

  @Override
  public <V> IScheduledTask<V> schedule(Callable<V> callable) {
    return this.schedule(callable, 0);
  }

  @Override
  public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay) {
    return this.schedule(callable, delay, TimeUnit.MILLISECONDS);
  }

  @Override
  public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, TimeUnit timeUnit) {
    return this.schedule(callable, delay, 0, timeUnit);
  }

  @Override
  public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat) {
    return this.schedule(callable, delay, repeat, 1);
  }

  @Override
  public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat, TimeUnit timeUnit) {
    return this.schedule(callable, delay, repeat, 1, timeUnit);
  }

  @Override
  public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat, long repeats) {
    return this.schedule(callable, delay, repeat, repeats, TimeUnit.MILLISECONDS);
  }

  @Override
  public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat, long repeats,
    TimeUnit timeUnit) {
    return this.offerTask(new DefaultScheduledTask<>(callable, delay, repeat, repeats, timeUnit));
  }

  @Override
  public IScheduledTask<Void> schedule(Runnable runnable) {
    return this.schedule(runnable, 0);
  }

  @Override
  public IScheduledTask<Void> schedule(Runnable runnable, long delay) {
    return this.schedule(runnable, delay, TimeUnit.MILLISECONDS);
  }

  @Override
  public IScheduledTask<Void> schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
    return this.schedule(runnable, delay, 0, timeUnit);
  }

  @Override
  public IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat) {
    return this.schedule(runnable, delay, repeat, TimeUnit.MILLISECONDS);
  }

  @Override
  public IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat, TimeUnit timeUnit) {
    return this.schedule(runnable, delay, repeat, 1, timeUnit);
  }

  @Override
  public IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat, long repeats) {
    return this.schedule(runnable, delay, repeat, repeats, TimeUnit.MILLISECONDS);
  }

  @Override
  public IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat, long repeats, TimeUnit timeUnit) {
    return this.schedule(new VoidCallable(runnable), delay, repeat, repeats, timeUnit);
  }

  @Override
  public void shutdown() {
    for (IWorkableThread worker : this.workers) {
      try {
        worker.stop();
      } catch (ThreadDeath th) {
        this.workers.remove(worker);
      }
    }

    this.taskEntries.clear();
    this.workers.clear();
  }

  @Override
  public void execute(@NotNull Runnable command) {
    this.schedule(command);
  }

  @Override
  public <V> IScheduledTask<V> offerTask(IScheduledTask<V> scheduledTask) {
    if (scheduledTask != null) {
      this.taskEntries.offer(scheduledTask);
      this.checkEnoughThreads();
    }

    return scheduledTask;
  }

  @Override
  public ITaskScheduler cancelAll() {
    for (IWorkableThread worker : this.workers) {
      try {
        worker.interrupt();
        worker.stop();
      } catch (ThreadDeath th) {
        this.workers.remove(worker);
      }
    }

    this.taskEntries.clear();
    this.workers.clear();
    return this;
  }

  private void checkEnoughThreads() {
    IWorkableThread workableThread = this.hasFreeWorker();

    if (workableThread == null && this.getCurrentWorkerCount() < this.maxThreadSize) {
      this.createWorker();
    }
  }


  private static final class VoidCallable implements Callable<Void> {

    private final Runnable runnable;

    public VoidCallable(Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    public Void call() {
      this.runnable.run();
      return null;
    }
  }

  private final class Worker extends Thread implements IWorkableThread {

    protected volatile IScheduledTask<?> scheduledTask = null;

    protected long lifeMillis = System.currentTimeMillis();

    public Worker() {
      super(DefaultTaskScheduler.this.threadGroup,
        DefaultTaskScheduler.this.threadGroup.getName() + "#" + DefaultTaskScheduler.this.threadCount
          .incrementAndGet());

      DefaultTaskScheduler.this.workers.add(this);

      this.setPriority(Thread.MIN_PRIORITY);
      this.setDaemon(true);
      this.start();
    }

    @Override
    public void run() {
      while (!this.isInterrupted() && (this.lifeMillis + DefaultTaskScheduler.this.threadLifeMillis) > System
        .currentTimeMillis()) {
        this.run0();
        this.sleep0(DefaultTaskScheduler.this.threadPauseDelayMillis);
      }

      DefaultTaskScheduler.this.workers.remove(this);
    }

    private synchronized void run0() {
      while (!DefaultTaskScheduler.this.taskEntries.isEmpty() && !this.isInterrupted()) {
        this.scheduledTask = DefaultTaskScheduler.this.taskEntries.poll();
        if (this.scheduledTask == null) {
          continue;
        }

        this.lifeMillis = System.currentTimeMillis();

        long difference = this.scheduledTask.getDelayedTimeStamp() - System.currentTimeMillis();
        if (difference > DefaultTaskScheduler.this.threadPauseDelayMillis) {
          this.sleep0(DefaultTaskScheduler.this.threadPauseDelayMillis - 1);
          this.offerEntry(this.scheduledTask);
          continue;
        } else if (difference > 0) {
          this.sleep0(difference);
        }

        try {
          this.scheduledTask.call();
        } catch (Throwable throwable) {
          throwable.printStackTrace();
        }

        if (this.checkScheduledTask()) {
          this.scheduledTask = null;
        }
      }
    }

    private boolean checkScheduledTask() {
      if (this.scheduledTask.isRepeatable()) {
        this.offerEntry(this.scheduledTask);
        return false;
      }

      return true;
    }

    private void sleep0(long value) {
      try {
        Thread.sleep(value);
      } catch (InterruptedException exception) {
        exception.printStackTrace();
      }
    }

    private void offerEntry(IScheduledTask<?> scheduledTask) {
      DefaultTaskScheduler.this.taskEntries.offer(scheduledTask);
      this.scheduledTask = null;
    }

    @Override
    public <V> IWorkableThread setTask(IScheduledTask<V> scheduledTask) {
      this.scheduledTask = scheduledTask;
      return this;
    }

    @Override
    public IScheduledTask<?> getTask() {
      return this.scheduledTask;
    }

    @Override
    public boolean isEmpty() {
      return this.scheduledTask == null;
    }

    @Override
    public int getTasksCount() {
      return 0;
    }

    @Override
    public void close() {
      this.interrupt();
    }
  }
}
