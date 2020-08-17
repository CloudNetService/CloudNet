package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

public class WrappedOutputStream extends OutputStream {

    private final OutputStream wrapped;

    public WrappedOutputStream(OutputStream wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void write(int b) throws IOException {
        wrapped.write(b);
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException {
        wrapped.write(b);
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        wrapped.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        wrapped.flush();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }
}
