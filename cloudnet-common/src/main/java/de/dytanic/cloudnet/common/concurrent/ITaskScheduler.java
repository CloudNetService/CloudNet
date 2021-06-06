package de.dytanic.cloudnet.common.concurrent;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.ApiStatus;

@Deprecated
@ApiStatus.ScheduledForRemoval
public interface ITaskScheduler extends IScheduledTaskInvoker, Executor {

  long getThreadLifeMillis();

  void setThreadLifeMillis(long threadLifeMillis);

  int getCurrentWorkerCount();

  int getMaxThreadSize();

  IWorkableThread createWorker();

  IWorkableThread hasFreeWorker();

  Collection<IWorkableThread> getWorkers();

  <V> IScheduledTask<V> schedule(Callable<V> callable);

  <V> IScheduledTask<V> schedule(Callable<V> callable, long delay);

  <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, TimeUnit timeUnit);

  <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat);

  <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat, TimeUnit timeUnit);

  <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat, long repeats);

  <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat, long repeats, TimeUnit timeUnit);

  IScheduledTask<Void> schedule(Runnable runnable);

  IScheduledTask<Void> schedule(Runnable runnable, long delay);

  IScheduledTask<Void> schedule(Runnable runnable, long delay, TimeUnit timeUnit);

  IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat);

  IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat, TimeUnit timeUnit);

  IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat, long repeats);

  IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat, long repeats, TimeUnit timeUnit);

  void shutdown();

}
