package de.dytanic.cloudnet.common.concurrent.scheduler;

import de.dytanic.cloudnet.common.annotation.UnsafeClass;
import de.dytanic.cloudnet.common.concurrent.IVoidCallback;

import java.time.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class, will be removed in the future, and exchanged
 * for a new TaskScheduler with the support of the ITask class.
 *
 * @see de.dytanic.cloudnet.common.concurrent.ITask
 * @see de.dytanic.cloudnet.common.concurrent.ITaskScheduler
 */
@Deprecated
@UnsafeClass
public class TaskScheduler {

    private static final AtomicLong THREAD_GROUP_ID_LONG = new AtomicLong();

    protected final ThreadGroup threadGroup = new ThreadGroup("TaskSchedulerGroup-" + THREAD_GROUP_ID_LONG.incrementAndGet());

    protected final AtomicLong threadId = new AtomicLong(0);

    protected final String name = threadGroup.getName();

    protected final long sleepThreadSwitch;

    protected final boolean dynamicWorkerCount;

    protected final long threadLifeMillis;

    protected int maxThreads;

    protected Deque<TaskEntry<?>> taskEntries = new ConcurrentLinkedDeque<>();

    protected Collection<Worker> workers = new ConcurrentLinkedQueue<>();


    public TaskScheduler() {
        this(Runtime.getRuntime().availableProcessors());
    }


    public TaskScheduler(long sleepThreadSwitch) {
        this(Runtime.getRuntime().availableProcessors(), sleepThreadSwitch);
    }


    public TaskScheduler(Collection<TaskEntry<?>> entries) {
        this(Runtime.getRuntime().availableProcessors(), entries);
    }


    public TaskScheduler(Collection<TaskEntry<?>> entries, long sleepThreadSwitch) {
        this(Runtime.getRuntime().availableProcessors(), entries, sleepThreadSwitch);
    }


    public TaskScheduler(int maxThreads, long sleepThreadSwitch) {
        this(maxThreads, null, sleepThreadSwitch);
    }


    public TaskScheduler(int maxThreads) {
        this(maxThreads, null);
    }


    public TaskScheduler(int maxThreads, boolean dynamicWorkerCount) {
        this(maxThreads, null, 10, dynamicWorkerCount);
    }


    public TaskScheduler(int maxThreads, Collection<TaskEntry<?>> entries) {
        this(maxThreads, entries, 10);
    }


    public TaskScheduler(int maxThreads, Collection<TaskEntry<?>> entries, boolean dynamicWorkerCount) {
        this(maxThreads, entries, 10, dynamicWorkerCount);
    }


    public TaskScheduler(int maxThreads, Collection<TaskEntry<?>> entries, long sleepThreadSwitch) {
        this(maxThreads, entries, sleepThreadSwitch, false);
    }

    public TaskScheduler(int maxThreads, Collection<TaskEntry<?>> entries, long sleepThreadSwitch, boolean dynamicThreadCount) {
        this(maxThreads, entries, sleepThreadSwitch, dynamicThreadCount, 10000L);
    }

    public TaskScheduler(int maxThreads, Collection<TaskEntry<?>> entries, long sleepThreadSwitch, boolean dynamicThreadCount, long threadLifeMillis) {

        this.sleepThreadSwitch = sleepThreadSwitch;
        this.dynamicWorkerCount = dynamicThreadCount;
        this.threadLifeMillis = threadLifeMillis;

        this.maxThreads = maxThreads <= 0 ? Runtime.getRuntime().availableProcessors() : maxThreads;

        if (entries != null) {
            taskEntries.addAll(entries);
        }
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable) {
        return schedule(runnable, (IVoidCallback<Void>) null);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, Date timeout) {
        return schedule(runnable, timeout.getTime() - System.currentTimeMillis());
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, LocalDate localDate, LocalTime localTime) {
        return schedule(runnable, null, localDate, localTime);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, LocalDateTime localDateTime) {
        return schedule(runnable, null, localDateTime);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, ZonedDateTime zonedDateTime) {
        return schedule(runnable, null, zonedDateTime);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, Instant instant) {
        return schedule(runnable, null, instant);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback) {
        return schedule(runnable, callback, 0);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, LocalDate localDate, LocalTime localTime) {
        return schedule(runnable, callback, localDate, localTime, 0);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, LocalDateTime localDateTime) {
        return schedule(runnable, callback, localDateTime, 0);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, LocalDateTime localDateTime, long repeats) {
        return schedule(runnable, callback, localDateTime.atZone(ZoneId.systemDefault()), repeats);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, LocalDate localDate, LocalTime localTime, long repeats) {
        return schedule(runnable, callback, LocalDateTime.of(localDate, localTime), repeats);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, ZonedDateTime zonedDateTime) {
        return schedule(runnable, callback, zonedDateTime, 0);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, Instant instant) {
        return schedule(runnable, callback, instant, 0);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, Date timeout) {
        return schedule(runnable, callback, timeout.getTime() - System.currentTimeMillis());
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, long delay) {
        return schedule(runnable, (IVoidCallback<Void>) null, delay);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        return schedule(runnable, (IVoidCallback<Void>) null, timeUnit.toMillis(delay));
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, long delay) {
        return schedule(runnable, callback, delay, 0);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, long delay, TimeUnit timeUnit) {
        return schedule(runnable, (IVoidCallback<Void>) null, timeUnit.toMillis(delay));
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, long delay, long repeats) {
        return schedule(runnable, null, delay, repeats);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, ZonedDateTime zonedDateTime, long repeats) {
        return schedule(runnable, callback, zonedDateTime.toInstant(), repeats);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, Instant instant, long repeats) {
        return schedule(runnable, callback, instant.toEpochMilli() - System.currentTimeMillis(), repeats);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, Date timeout, long repeats) {
        return schedule(runnable, timeout.getTime() - System.currentTimeMillis(), repeats);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, long delay, TimeUnit timeUnit, long repeats) {
        return schedule(runnable, null, timeUnit.toMillis(delay), repeats);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, Date timeout, long repeats) {
        return schedule(runnable, callback, timeout.getTime() - System.currentTimeMillis(), repeats);
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, long delay, long repeats) {
        return schedule(new VoidTaskEntry(runnable, callback, delay, repeats));
    }


    public TaskEntryFuture<Void> schedule(Runnable runnable, IVoidCallback<Void> callback, long delay, TimeUnit timeUnit, long repeats) {
        return schedule(runnable, callback, timeUnit.toMillis(delay), repeats);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable) {
        return schedule(callable, (IVoidCallback<V>) null);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, LocalDate localDate, LocalTime localTime) {
        return schedule(callable, null, localDate, localTime);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, LocalDateTime localDateTime) {
        return schedule(callable, null, localDateTime);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, ZonedDateTime zonedDateTime) {
        return schedule(callable, null, zonedDateTime);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, Instant instant) {
        return schedule(callable, null, instant);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, long delay) {
        return schedule(callable, null, delay);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, long delay, TimeUnit timeUnit) {
        return schedule(callable, null, timeUnit.toMillis(delay));
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback) {
        return schedule(callable, callback, 0);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, LocalDate localDate, LocalTime localTime) {
        return schedule(callable, callback, localDate, localTime, 0);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, LocalDateTime localDateTime) {
        return schedule(callable, callback, localDateTime, 0);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, ZonedDateTime zonedDateTime) {
        return schedule(callable, callback, zonedDateTime, 0);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, Instant instant) {
        return schedule(callable, callback, instant, 0);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, long delay) {
        return schedule(callable, callback, delay, 0);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, long delay, TimeUnit timeUnit) {
        return schedule(callable, callback, timeUnit.toMillis(delay));
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, long delay, long repeats) {
        return schedule(new TaskEntry<>(callable, callback, delay, repeats));
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, long delay, TimeUnit timeUnit, long repeats) {
        return schedule(callable, callback, timeUnit.toMillis(delay), repeats);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, LocalDate localDate, LocalTime localTime, long repeats) {
        return schedule(callable, callback, LocalDateTime.of(localDate, localTime), repeats);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, LocalDateTime localDateTime, long repeats) {
        return schedule(callable, callback, localDateTime.atZone(ZoneId.systemDefault()), 0);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, ZonedDateTime zonedDateTime, long repeats) {
        return schedule(callable, callback, zonedDateTime.toInstant(), 0);
    }


    public <V> TaskEntryFuture<V> schedule(Callable<V> callable, IVoidCallback<V> callback, Instant instant, long repeats) {
        return schedule(callable, callback, instant.toEpochMilli(), 0);
    }


    public <V> TaskEntryFuture<V> schedule(TaskEntry<V> taskEntry) {
        return offerEntry(taskEntry);
    }


    public <V> Collection<TaskEntryFuture<V>> schedule(Collection<TaskEntry<V>> threadEntries) {

        Collection<TaskEntryFuture<V>> TaskEntryFutures = new ArrayList<>();
        for (TaskEntry<V> entry : threadEntries) {
            TaskEntryFutures.add(offerEntry(entry));
        }

        return TaskEntryFutures;
    }


    protected void newWorker() {
        Worker worker = new Worker();
        workers.add(worker);

        worker.start();
    }


    @SuppressWarnings("deprecation")
    public Collection<TaskEntry<?>> shutdown() {

        for (Worker worker : workers) {
            try {
                worker.interrupt();
                worker.stop();
            } catch (ThreadDeath th) {
                workers.remove(worker);
            }
        }

        Collection<TaskEntry<?>> entries = new ArrayList<>(taskEntries);

        taskEntries.clear();
        workers.clear();
        threadId.set(0);

        return entries;
    }


    public TaskScheduler chargeThreadLimit(short threads) {
        this.maxThreads += threads;
        return this;
    }


    public int getCurrentThreadSize() {
        return this.workers.size();
    }


    public int getMaxThreads() {
        return maxThreads;
    }


    public ThreadGroup getThreadGroup() {
        return threadGroup;
    }


    public String getName() {
        return name;
    }


    public Deque<TaskEntry<?>> getThreadEntries() {
        return new ConcurrentLinkedDeque<>();
    }


    private void checkEnoughThreads() {
        Worker worker = hasFreeWorker();
        if (getCurrentThreadSize() < maxThreads
                || (dynamicWorkerCount && maxThreads > 1 && taskEntries.size() > getCurrentThreadSize() && taskEntries.size() <= (getMaxThreads() * 2)) && worker == null) {
            newWorker();
        }
    }

    private Worker hasFreeWorker() {
        for (Worker worker : workers) {
            if (worker.isFreeWorker()) {
                return worker;
            }
        }

        return null;
    }

    private <V> TaskEntryFuture<V> offerEntry(TaskEntry<V> entry) {
        this.taskEntries.offer(entry);
        checkEnoughThreads();
        return entry.drop();
    }

    private static final class VoidTaskEntry extends TaskEntry<Void> {

        public VoidTaskEntry(Callable<Void> task, IVoidCallback<Void> complete, long delay, long repeat) {
            super(task, complete, delay, repeat);
        }


        public VoidTaskEntry(Runnable task, IVoidCallback<Void> complete, long delay, long repeat) {
            super(() -> {

                if (task != null) {
                    task.run();
                }

                return null;
            }, complete, delay, repeat);
        }
    }

    public class Worker extends Thread {

        volatile TaskEntry<?> taskEntry = null;

        private long liveTimeStamp = System.currentTimeMillis();

        Worker() {
            super(threadGroup, threadGroup.getName() + "#" + threadId.incrementAndGet());
            setDaemon(true);
        }

        public boolean isFreeWorker() {
            return taskEntry == null;
        }

        @Override
        public synchronized void run() {
            while ((liveTimeStamp + threadLifeMillis) > System.currentTimeMillis()) {
                execute();
                sleepUninterruptedly(sleepThreadSwitch);
            }

            workers.remove(this);
        }

        public synchronized void execute() {
            while (!taskEntries.isEmpty() && !isInterrupted()) {
                taskEntry = taskEntries.poll();

                if (taskEntry == null || taskEntry.task == null) {
                    continue;
                }

                liveTimeStamp = System.currentTimeMillis();

                if (taskEntry.delayTimeOut != 0 && System.currentTimeMillis() < taskEntry.delayTimeOut) {
                    if (maxThreads != 1) {
                        long difference = taskEntry.delayTimeOut - System.currentTimeMillis();

                        if (difference > sleepThreadSwitch) {
                            sleepUninterruptedly(sleepThreadSwitch - 1);
                            offerEntry(taskEntry);
                            continue;

                        } else {
                            sleepUninterruptedly(difference);
                        }
                    } else {
                        sleepUninterruptedly(sleepThreadSwitch);
                        offerEntry(taskEntry);
                        continue;
                    }
                }

                try {
                    taskEntry.invoke();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (checkEntry()) {
                    taskEntry = null;
                }
            }
        }

        public TaskEntry<?> getTaskEntry() {
            return taskEntry;
        }


        private void offerEntry(TaskEntry<?> entry) {
            taskEntries.offer(taskEntry);
            taskEntry = null;
        }


        private boolean checkEntry() {
            if (taskEntry.repeat == -1) {
                offerEntry(taskEntry);
                return false;
            }

            if (taskEntry.repeat > 0) {
                offerEntry(taskEntry);
                return false;
            }

            return true;
        }


        private synchronized void sleepUninterruptedly(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }

    }

}