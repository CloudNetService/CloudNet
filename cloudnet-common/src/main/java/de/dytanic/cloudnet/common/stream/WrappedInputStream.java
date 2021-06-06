package de.dytanic.cloudnet.common.stream;

import java.io.IOException;
import java.io.InputStream;
import org.jetbrains.annotations.NotNull;

public class WrappedInputStream extends InputStream {

  private final InputStream wrapped;

  public WrappedInputStream(InputStream wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public int read() throws IOException {
    return this.wrapped.read();
  }

  @Override
  public int read(@NotNull byte[] b) throws IOException {
    return this.wrapped.read(b);
  }

  @Override
  public int read(@NotNull byte[] b, int off, int len) throws IOException {
    return this.wrapped.read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return this.wrapped.skip(n);
  }

  @Override
  public int available() throws IOException {
    return this.wrapped.available();
  }

  @Override
  public void close() throws IOException {
    this.wrapped.close();
  }

  @Override
  public boolean markSupported() {
    return this.wrapped.markSupported();
  }

  @Override
  public synchronized void mark(int readlimit) {
    this.wrapped.mark(readlimit);
  }

  @Override
  public synchronized void reset() throws IOException {
    this.wrapped.reset();
  }
}
