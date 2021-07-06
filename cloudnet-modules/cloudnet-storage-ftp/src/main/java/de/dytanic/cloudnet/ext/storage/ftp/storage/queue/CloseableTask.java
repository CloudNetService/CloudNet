/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableFunction;
import de.dytanic.cloudnet.common.stream.WrappedInputStream;
import de.dytanic.cloudnet.common.stream.WrappedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

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
