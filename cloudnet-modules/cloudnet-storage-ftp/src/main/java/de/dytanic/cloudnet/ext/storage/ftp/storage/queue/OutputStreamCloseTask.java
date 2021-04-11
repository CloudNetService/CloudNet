package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@NotNull
public class OutputStreamCloseTask extends OutputStream implements ITask<OutputStream> {

    private final OutputStream outputStream;
    private boolean done;

    OutputStreamCloseTask(OutputStream outputStream) {
        Preconditions.checkNotNull(outputStream, "OutputStream is null!");

        this.outputStream = outputStream;
    }

    @Override
    public void write(int b) throws IOException {
        this.outputStream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.outputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        this.outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        this.call();
        this.outputStream.close();
    }

    @Override
    @NotNull
    public ITask<OutputStream> addListener(ITaskListener<OutputStream> listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public ITask<OutputStream> clearListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ITaskListener<OutputStream>> getListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Callable<OutputStream> getCallable() {
        return () -> this.outputStream;
    }

    @Override
    public OutputStream getDef(OutputStream def) {
        return this.get();
    }

    @Override
    public OutputStream get(long time, TimeUnit timeUnit, OutputStream def) {
        return this.get();
    }

    @Override
    public <T> ITask<T> map(Function<OutputStream, T> mapper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream call() {
        this.done = true;

        synchronized (this) {
            try {
                this.notifyAll();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        return this.outputStream;
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
    public OutputStream get() {
        synchronized (this) {
            if (!this.isDone()) {
                try {
                    this.wait();
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        }

        return this.outputStream;
    }

    @Override
    public OutputStream get(long timeout, @NotNull TimeUnit unit) {
        return this.get();
    }


}
