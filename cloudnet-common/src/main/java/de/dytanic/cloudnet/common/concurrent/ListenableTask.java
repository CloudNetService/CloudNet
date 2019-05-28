package de.dytanic.cloudnet.common.concurrent;

import de.dytanic.cloudnet.common.Validate;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.Getter;

@Getter
public class ListenableTask<V> implements ITask<V> {

  @Getter
  private Collection<ITaskListener<V>> listeners;

  private volatile V value;

  private volatile boolean done, cancelled;

  @Getter
  private final Callable<V> callable;

  public ListenableTask(Callable<V> callable) {
    this(callable, null);
  }

  public ListenableTask(Callable<V> callable, ITaskListener<V> listener) {
    Validate.checkNotNull(callable);

    this.callable = callable;

    if (listener != null) {
      this.addListener(listener);
    }
  }

  @Override
  public ITask<V> addListener(ITaskListener<V>... listeners) {
    if (listeners == null) {
      return this;
    }

    initListenersCollectionIfNotExists();

    for (ITaskListener<V> listener : listeners) {
      if (listener != null) {
        this.listeners.add(listener);
      }
    }

    return this;
  }

  @Override
  public ITask<V> clearListeners() {
    if (this.listeners != null) {
      this.listeners.clear();
    }

    return this;
  }

  @Override
  public V getDef(V def) {
    return get(5, TimeUnit.SECONDS, def);
  }

  @Override
  public V get(long time, TimeUnit timeUnit, V def) {
    Validate.checkNotNull(timeUnit);

    try {
      return get(time, timeUnit);
    } catch (Throwable ignored) {
      ignored.printStackTrace();
    }

    return def;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return this.cancelled = mayInterruptIfRunning;
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    synchronized (this) {
      if (!isDone()) {
        this.wait();
      }
    }

    return value;
  }

  @Override
  public V get(long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException {
    synchronized (this) {
      if (!isDone()) {
        this.wait(unit.toMillis(timeout));
      }
    }

    return value;
  }

  /*= ------------------------------------------------------------------------------------------------------ =*/

  @Override
  public V call() {
    if (!isCancelled()) {
      try {
        this.value = this.callable.call();
      } catch (Throwable ex) {
        this.invokeFailure(ex);
      }
    }

    this.done = true;
    this.invokeTaskListener();

    synchronized (this) {
      try {
        this.notifyAll();
      } catch (Throwable ignored) {
      }
    }

    return this.value;
  }

  /*= ---------------------------------------------------------------------------------- =*/

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

  private void invokeFailure(Throwable ex) {
    if (this.listeners != null) {
      for (ITaskListener<V> listener : this.listeners) {
        try {
          listener.onFailure(this, ex);
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }
    }
  }
}