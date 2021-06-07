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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableFunction;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@Deprecated
@ApiStatus.ScheduledForRemoval
public final class DefaultScheduledTask<V> implements IScheduledTask<V> {

  private static final AtomicLong TASK_ID_COUNTER = new AtomicLong();

  private final long taskId = TASK_ID_COUNTER.incrementAndGet();

  private Collection<ITaskListener<V>> listeners;

  private volatile V value;
  private Callable<V> callable;

  private volatile boolean wait;
  private volatile boolean done;
  private volatile boolean cancelled;

  private long delay;
  private long repeat;
  private long repeats;
  private long delayedTimeStamp;

  public DefaultScheduledTask(Callable<V> callable, long delay, long repeat, long repeats, TimeUnit timeUnit) {
    this.callable = callable;

    this.delay = delay > 0 ? timeUnit.toMillis(delay) : -1;
    this.repeat = repeat > 0 ? timeUnit.toMillis(repeat) : -1;

    this.repeats = repeats;
    this.delayedTimeStamp = System.currentTimeMillis() + this.delay;
  }

  @Override
  @NotNull
  public final ITask<V> addListener(ITaskListener<V> listener) {
    if (listener == null) {
      return this;
    }

    this.initListenersCollectionIfNotExists();

    this.listeners.add(listener);

    return this;
  }

  @Override
  public long getTaskId() {
    return this.taskId;
  }

  @Override
  public Collection<ITaskListener<V>> getListeners() {
    return this.listeners;
  }

  public V getValue() {
    return this.value;
  }

  public boolean isWait() {
    return this.wait;
  }

  public void setWait(boolean wait) {
    this.wait = wait;
  }

  @Override
  public boolean isDone() {
    return this.done;
  }

  public void setDone(boolean done) {
    this.done = done;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  public long getDelay() {
    return this.delay;
  }

  public long getRepeat() {
    return this.repeat;
  }

  public long getRepeats() {
    return this.repeats;
  }

  @Override
  public long getDelayedTimeStamp() {
    return this.delayedTimeStamp;
  }

  @Override
  public Callable<V> getCallable() {
    return this.callable;
  }

  @Override
  @NotNull
  public ITask<V> clearListeners() {
    this.listeners.clear();
    return this;
  }

  @Override
  public synchronized V getDef(V def) {
    return this.get(5, TimeUnit.SECONDS, def);
  }

  @Override
  public synchronized V get(long time, TimeUnit timeUnit, V def) {
    Preconditions.checkNotNull(timeUnit);

    try {
      return this.get(time, timeUnit);
    } catch (Throwable throwable) {
      return def;
    }
  }

  @Override
  public <T> ITask<T> mapThrowable(ThrowableFunction<V, T, Throwable> mapper) {
    return CompletableTask.mapFrom(this, mapper);
  }

  @Override
  public V call() {
    if (this.callable == null || this.done) {
      return this.value;
    }

    if (!this.isCancelled()) {
      try {
        this.value = this.callable.call();
      } catch (Throwable throwable) {
        this.invokeFailure(throwable);
      }
    }

    if (this.repeats > 0) {
      this.repeats--;
    }

    if ((this.repeats > 0 || this.repeats == -1) && !this.cancelled) {
      this.delayedTimeStamp = System.currentTimeMillis() + this.repeat;
    } else {
      this.done = true;
      this.invokeTaskListener();

      if (this.wait) {
        synchronized (this) {
          this.notifyAll();
        }
      }
    }

    return this.value;
  }


  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (mayInterruptIfRunning) {
      this.callable = null;
      this.repeats = 0;
    }

    return mayInterruptIfRunning;
  }

  @Override
  public synchronized V get() throws InterruptedException {
    this.wait = true;
    while (!this.isDone()) {
      this.wait();
    }

    return this.value;
  }

  @Override
  public synchronized V get(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
    this.wait = true;
    if (!this.isDone()) {
      this.wait(unit.toMillis(timeout));
    }

    return this.value;
  }


  private void initListenersCollectionIfNotExists() {
    if (this.listeners == null) {
      this.listeners = new ConcurrentLinkedQueue<>();
    }
  }

  private void invokeTaskListener() {
    if (this.listeners != null) {
      for (ITaskListener<V> listener : this.listeners) {
        try {
          if (this.cancelled) {
            listener.onCancelled(this);
          } else {
            listener.onComplete(this, this.value);
          }
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }
    }
  }

  private void invokeFailure(Throwable throwable) {
    if (this.listeners != null) {
      for (ITaskListener<V> listener : this.listeners) {
        try {
          listener.onFailure(this, throwable);
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }
    }
  }

  @Override
  public boolean isRepeatable() {
    return this.repeats > 0 || this.repeats == -1;
  }

  @Override
  public IScheduledTask<V> setDelayMillis(long delayMillis) {
    this.delay = delayMillis;
    return this;
  }

  @Override
  public long getDelayMillis() {
    return this.delay;
  }

  @Override
  public IScheduledTask<V> setRepeatMillis(long repeatMillis) {
    this.repeat = repeatMillis;
    return this;
  }

  @Override
  public long getRepeatMillis() {
    return this.repeat;
  }

  @Override
  public IScheduledTask<V> cancel() {
    this.cancel(true);
    return this;
  }
}
