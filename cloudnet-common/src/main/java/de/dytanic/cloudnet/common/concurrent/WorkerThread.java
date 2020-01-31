package de.dytanic.cloudnet.common.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class WorkerThread extends Thread implements ExecutorService {

    protected final BlockingQueue<ITask<?>> tasks = new LinkedBlockingQueue<>();
    protected final long lifeMillis;
    protected volatile boolean available = true;
    protected long destinationTime;

    public WorkerThread() {
        super();
        this.lifeMillis = 30000;
        this.updateDestinationTime();
    }

    public WorkerThread(String name) {
        super(name);
        this.lifeMillis = 30000;
        this.updateDestinationTime();
    }

    public WorkerThread(ThreadGroup group, String name) {
        super(group, name);
        this.lifeMillis = 30000;
        this.updateDestinationTime();
    }

    public WorkerThread(ThreadGroup group, String name, long lifeMillis) {
        super(group, name);
        this.lifeMillis = lifeMillis;
        this.updateDestinationTime();
    }

    public boolean isAvailable() {
        return available;
    }

    public void clearAllTasks() {
        this.tasks.clear();
    }

    public <T> ITask submit(Callable<T> task, ITaskListener<T> listener) {
        return this.submit(task, new ITaskListener[]{listener});
    }

    public <T> ITask<T> submit(Callable<T> task, ITaskListener<T>[] listeners) {
        if (task == null) {
            return null;
        }

        ITask<T> taskListener = new ListenableTask<>(task);
        taskListener.addListener(listeners);

        this.tasks.offer(taskListener);
        return taskListener;
    }


    @Override
    public void shutdown() {
        this.interrupt();
    }

    @NotNull
    @Deprecated
    @Override
    public List<Runnable> shutdownNow() {
        this.shutdown();
        this.stop();

        return new ArrayList<>();
    }

    @Override
    public boolean isShutdown() {
        return this.isInterrupted();
    }

    @Override
    public boolean isTerminated() {
        return this.isInterrupted();
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        this.join();
        return true;
    }

    @NotNull
    @Override
    public <T> ITask<T> submit(@NotNull Callable<T> task) {
        return this.submit(task, new ITaskListener[0]);
    }

    @NotNull
    @Override
    public <T> ITask<T> submit(@NotNull Runnable task, T result) {
        return this.submit(Executors.callable(task, result));
    }

    @NotNull
    @Override
    public ITask<?> submit(@NotNull Runnable task) {
        return (ITask<?>) Executors.callable(task);
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        List<Future<T>> list = new ArrayList<>(tasks.size());

        for (Callable<T> callable : tasks) {
            list.add(this.submit(callable));
        }
        return list;
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) {
        return this.invokeAll(tasks);
    }

    @NotNull
    @Deprecated
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) {
        throw new UnsupportedOperationException("Method invokeAny(Collection<? extends Callable<T>> tasks) won't support");
    }

    @Deprecated
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) {
        throw new UnsupportedOperationException("Method invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) won't support");
    }

    @Override
    public final void execute(@NotNull Runnable command) {
        this.submit(command);
    }


    @Override
    public final void run() {
        while (!isInterrupted()) {
            try {
                this.available = true;

                ITask<?> task = this.tasks.take();

                this.available = false;

                try {
                    task.call();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }

                if (this.destinationTime > System.currentTimeMillis()) {
                    this.updateDestinationTime();
                } else {
                    break;
                }

            } catch (Throwable ex) {
                break;
            }
        }

        this.available = false;

        while (!this.tasks.isEmpty()) {
            try {
                this.tasks.take().call();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        this.postRun();
    }

    protected void postRun() {

    }


    protected final void updateDestinationTime() {
        this.destinationTime = System.currentTimeMillis() + this.lifeMillis;
    }

}