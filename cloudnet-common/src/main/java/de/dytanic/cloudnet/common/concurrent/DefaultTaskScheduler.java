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

public class DefaultTaskScheduler implements ITaskScheduler {

    protected static final long DEFAULT_THREAD_LIFE_MILLIS = 60000, DEFAULT_THREAD_PAUSE_MILLIS = 5;

    protected static final AtomicInteger GROUP_COUNT = new AtomicInteger();

    protected final Deque<IScheduledTask<?>> taskEntries = new ConcurrentLinkedDeque<>();

    protected final Queue<IWorkableThread> workers = new ConcurrentLinkedQueue<>();

    protected final ThreadGroup threadGroup = new ThreadGroup("DefaultTaskScheduler-" + GROUP_COUNT.incrementAndGet());

    protected final AtomicLong THREAD_COUNT = new AtomicLong();

    protected volatile int maxThreadSize;

    public void setMaxThreadSize(int maxThreadSize) {
        this.maxThreadSize = maxThreadSize;
    }

    protected volatile long threadLifeMillis;

    public void setThreadLifeMillis(long threadLifeMillis) {
        this.threadLifeMillis = threadLifeMillis;
    }

    protected volatile long threadPauseDelayMillis;

    public void setThreadPauseDelayMillis(long threadPauseDelayMillis) {
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
        return taskEntries;
    }

    public ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    public AtomicLong getTHREAD_COUNT() {
        return THREAD_COUNT;
    }

    public int getMaxThreadSize() {
        return maxThreadSize;
    }

    public long getThreadLifeMillis() {
        return threadLifeMillis;
    }

    public long getThreadPauseDelayMillis() {
        return threadPauseDelayMillis;
    }

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
        for (IWorkableThread workableThread : this.workers)
            if (workableThread.isEmpty())
                return workableThread;

        return null;
    }

    @Override
    public Collection<IWorkableThread> getWorkers() {
        return this.workers;
    }

    @Override
    public <V> IScheduledTask<V> schedule(Callable<V> callable) {
        return schedule(callable, 0);
    }

    @Override
    public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay) {
        return schedule(callable, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, TimeUnit timeUnit) {
        return schedule(callable, delay, 0, timeUnit);
    }

    @Override
    public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat) {
        return schedule(callable, delay, repeat, 1);
    }

    @Override
    public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat, TimeUnit timeUnit) {
        return schedule(callable, delay, repeat, -1, timeUnit);
    }

    @Override
    public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat, long repeats) {
        return schedule(callable, delay, repeat, repeats, TimeUnit.MILLISECONDS);
    }

    @Override
    public <V> IScheduledTask<V> schedule(Callable<V> callable, long delay, long repeat, long repeats, TimeUnit timeUnit) {
        return offerTask(new DefaultScheduledTask<>(callable, delay, repeat, repeats, timeUnit));
    }

    @Override
    public IScheduledTask<Void> schedule(Runnable runnable) {
        return schedule(runnable, 0);
    }

    @Override
    public IScheduledTask<Void> schedule(Runnable runnable, long delay) {
        return schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public IScheduledTask<Void> schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        return schedule(runnable, delay, 0, timeUnit);
    }

    @Override
    public IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat) {
        return schedule(runnable, delay, repeat, TimeUnit.MILLISECONDS);
    }

    @Override
    public IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat, TimeUnit timeUnit) {
        return schedule(runnable, delay, repeat, -1, timeUnit);
    }

    @Override
    public IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat, long repeats) {
        return schedule(runnable, delay, repeat, repeats, TimeUnit.MILLISECONDS);
    }

    @Override
    public IScheduledTask<Void> schedule(Runnable runnable, long delay, long repeat, long repeats, TimeUnit timeUnit) {
        return schedule(new VoidCallable(runnable), delay, repeat, repeats, timeUnit);
    }

    @Override
    public void shutdown() {
        for (IWorkableThread worker : this.workers)
            try {
                worker.stop();
            } catch (ThreadDeath th) {
                workers.remove(worker);
            }

        taskEntries.clear();
        workers.clear();
    }

    @Override
    public void execute(Runnable command) {
        schedule(command);
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
        for (IWorkableThread worker : this.workers)
            try {
                worker.interrupt();
                worker.stop();
            } catch (ThreadDeath th) {
                workers.remove(worker);
            }

        taskEntries.clear();
        workers.clear();
        return this;
    }

    private void checkEnoughThreads() {
        IWorkableThread workableThread = hasFreeWorker();

        if (workableThread == null && this.getCurrentWorkerCount() < maxThreadSize)
            this.createWorker();
    }

    /*= ------------------------------------------------------------- =*/

    private final class VoidCallable implements Callable<Void> {

        private final Runnable runnable;

        public VoidCallable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public Void call() throws Exception {
            runnable.run();
            return null;
        }
    }

    private final class Worker extends Thread implements IWorkableThread {

        protected volatile IScheduledTask<?> scheduledTask = null;

        protected long lifeMillis = System.currentTimeMillis();

        public Worker() {
            super(threadGroup, threadGroup.getName() + "#" + THREAD_COUNT.incrementAndGet());

            workers.add(this);

            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
            start();
        }

        @Override
        public void run() {
            while (!isInterrupted() && (lifeMillis + threadLifeMillis) > System.currentTimeMillis()) {
                this.run0();
                this.sleep0(threadPauseDelayMillis);
            }

            workers.remove(this);
        }

        private synchronized void run0() {
            while (!taskEntries.isEmpty() && !isInterrupted()) {
                scheduledTask = taskEntries.poll();

                if (scheduledTask == null) continue;

                lifeMillis = System.currentTimeMillis();

                long difference = scheduledTask.getDelayedTimeStamp() - System.currentTimeMillis();

                if (difference > threadPauseDelayMillis) {
                    sleep0(threadPauseDelayMillis - 1);
                    offerEntry(scheduledTask);
                    continue;

                } else if (difference > 0)
                    sleep0(difference);

                try {
                    scheduledTask.call();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

                if (checkScheduledTask()) scheduledTask = null;
            }
        }

        private boolean checkScheduledTask() {
            if (scheduledTask.isRepeatable()) {
                this.offerEntry(scheduledTask);
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
            taskEntries.offer(scheduledTask);
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
        public void close() throws Exception {
            this.stop();
        }
    }
}