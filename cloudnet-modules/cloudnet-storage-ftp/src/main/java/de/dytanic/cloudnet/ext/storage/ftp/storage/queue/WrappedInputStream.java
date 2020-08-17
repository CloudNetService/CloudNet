package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

public class WrappedInputStream extends InputStream {

    private final InputStream wrapped;

    public WrappedInputStream(InputStream wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public int read() throws IOException {
        return wrapped.read();
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return wrapped.read(b);
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        return wrapped.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return wrapped.skip(n);
    }

    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public void mark(int readlimit) {
        wrapped.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        wrapped.reset();
    }
}
