package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;


import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableFunction;
import de.dytanic.cloudnet.common.stream.WrappedInputStream;
import de.dytanic.cloudnet.common.stream.WrappedOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@NotNull
public class CloseableTask<C extends Closeable> implements ITask<C>, Closeable {

    private final C closeable;
    private boolean done;

    CloseableTask(C closeable) {
        Preconditions.checkNotNull(closeable, "OutputStream is null!");

        this.closeable = closeable;
    }

    public OutputStream toOutputStream() {
        return new WrappedOutputStream((OutputStream) this.closeable) {
            @Override
            public void close() throws IOException {
                super.close();
                CloseableTask.this.close();
            }
        };
    }

    public InputStream toInputStream() {
        return new WrappedInputStream((InputStream) this.closeable) {
            @Override
            public void close() throws IOException {
                super.close();
                CloseableTask.this.close();
            }
        };
    }

    @Override
    public void close() throws IOException {
        this.call();
        this.closeable.close();
    }

    @Override
    @NotNull
    public ITask<C> addListener(ITaskListener<C> listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public ITask<C> clearListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ITaskListener<C>> getListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Callable<C> getCallable() {
        return () -> this.closeable;
    }

    @Override
    public C getDef(C def) {
        return this.get();
    }

    @Override
    public C get(long time, TimeUnit timeUnit, C def) {
        return this.get();
    }

    @Override
    public <T> ITask<T> mapThrowable(ThrowableFunction<C, T, Throwable> mapper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public C call() {
        this.done = true;

        synchronized (this) {
            try {
                this.notifyAll();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        return this.closeable;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return this.done;
    }

    @Override
    public C get() {
        synchronized (this) {
            if (!this.isDone()) {
                try {
                    this.wait();
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        }

        return this.closeable;
    }

    @Override
    public C get(long timeout, @NotNull TimeUnit unit) {
        return this.get();
    }


}
