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

package de.dytanic.cloudnet.common.logging;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.jetbrains.annotations.NotNull;

/**
 * All println and print methods can be executed completely asynchronously at each instance of this class, and these are
 * then executed synchronously in the single thread. It should optimize performance and avoid blocking between thread
 * contexts
 * <p>
 * The actual console output is still executed in a thread where its priority is as low as possible to affect the
 * program even less
 */
@Deprecated
public class AsyncPrintStream extends PrintStream {

  static final BlockingQueue<Runnable> ASYNC_QUEUE = new LinkedBlockingQueue<>();
  private static final Thread worker = new Thread() {

    {
      this.setName("AsyncPrint-Thread");
      this.setPriority(Thread.MIN_PRIORITY);
      this.setDaemon(true);
      this.start();
    }

    @Override
    public void run() {
      while (!this.isInterrupted()) {
        try {
          Runnable runnable = ASYNC_QUEUE.take();
          runnable.run();
        } catch (InterruptedException exception) {
          exception.printStackTrace();
        }
      }
    }
  };

  public AsyncPrintStream(OutputStream out) throws UnsupportedEncodingException {
    super(out, true, StandardCharsets.UTF_8.name());
  }

  public static BlockingQueue<Runnable> getAsyncQueue() {
    return ASYNC_QUEUE;
  }

  private void println0() {
    super.println();
  }

  @Override
  public void println() {
    ASYNC_QUEUE.offer(this::println0);
  }

  private void println0(int x) {
    super.println(x);
  }

  @Override
  public void println(int x) {
    ASYNC_QUEUE.offer(() -> this.println0(x));
  }

  private void println0(String x) {
    super.println(x);
  }

  @Override
  public void println(String x) {
    ASYNC_QUEUE.offer(() -> this.println0(x));
  }

  private void println0(long x) {
    super.println(x);
  }

  @Override
  public void println(long x) {
    ASYNC_QUEUE.offer(() -> this.println0(x));
  }

  private void println0(char x) {
    super.println(x);
  }

  @Override
  public void println(char x) {
    ASYNC_QUEUE.offer(() -> this.println0(x));
  }

  private void println0(double x) {
    super.println(x);
  }

  @Override
  public void println(double x) {
    ASYNC_QUEUE.offer(() -> this.println0(x));
  }

  private void println0(float x) {
    super.println(x);
  }

  @Override
  public void println(float x) {
    ASYNC_QUEUE.offer(() -> this.println0(x));
  }

  private void println0(Object x) {
    super.println(x);
  }

  @Override
  public void println(Object x) {
    ASYNC_QUEUE.offer(() -> this.println0(x));
  }

  private void println0(char[] x) {
    super.println(x);
  }

  @Override
  public void println(@NotNull char[] x) {
    ASYNC_QUEUE.offer(() -> this.println0(x));
  }

  private void println0(boolean x) {
    super.println(x);
  }

  @Override
  public void println(boolean x) {
    ASYNC_QUEUE.offer(() -> this.println0(x));
  }


  private void print0(int x) {
    super.print(x);
  }

  @Override
  public void print(int x) {
    if (!this.isWorkerThread()) {
      ASYNC_QUEUE.offer(() -> this.print0(x));
    } else {
      super.print(x);
    }
  }

  private void print0(String x) {
    super.print(x);
  }

  @Override
  public void print(String x) {
    if (!this.isWorkerThread()) {
      ASYNC_QUEUE.offer(() -> this.print0(x));
    } else {
      super.print(x);
    }
  }

  private void print0(long x) {
    super.print(x);
  }

  @Override
  public void print(long x) {
    if (!this.isWorkerThread()) {
      ASYNC_QUEUE.offer(() -> this.print0(x));
    } else {
      super.print(x);
    }
  }

  private void print0(char x) {
    super.print(x);
  }

  @Override
  public void print(char x) {
    if (!this.isWorkerThread()) {
      ASYNC_QUEUE.offer(() -> this.print0(x));
    } else {
      super.print(x);
    }
  }

  private void print0(double x) {
    super.print(x);
  }

  @Override
  public void print(double x) {
    if (!this.isWorkerThread()) {
      ASYNC_QUEUE.offer(() -> this.print0(x));
    } else {
      super.print(x);
    }
  }

  private void print0(float x) {
    super.print(x);
  }

  @Override
  public void print(float x) {
    if (!this.isWorkerThread()) {
      ASYNC_QUEUE.offer(() -> this.print0(x));
    } else {
      super.print(x);
    }
  }

  private void print0(Object x) {
    super.print(x);
  }

  @Override
  public void print(Object x) {
    if (!this.isWorkerThread()) {
      ASYNC_QUEUE.offer(() -> this.print0(x));
    } else {
      super.print(x);
    }
  }

  private void print0(char[] x) {
    super.print(x);
  }

  @Override
  public void print(@NotNull char[] x) {
    if (!this.isWorkerThread()) {
      ASYNC_QUEUE.offer(() -> this.print0(x));
    } else {
      super.print(x);
    }
  }

  private void print0(boolean x) {
    super.print(x);
  }

  @Override
  public void print(boolean x) {
    if (!this.isWorkerThread()) {
      ASYNC_QUEUE.offer(() -> this.print0(x));
    } else {
      super.print(x);
    }
  }


  private boolean isWorkerThread() {
    return Thread.currentThread() == worker;
  }
}
