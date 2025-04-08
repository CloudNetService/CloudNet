/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.utils.base.io;

import io.vavr.CheckedConsumer;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.OutputStream;
import lombok.NonNull;

/**
 * An output stream implementation that holds a listener to call when the stream gets closed. The listener function can
 * for example be used to actually commit an action to the real underlying file system.
 *
 * @param <O> the type of the wrapped output stream.
 * @since 4.0
 */
public final class ListenableOutputStream<O extends OutputStream> extends OutputStream {

  private final O wrapped;
  private final CheckedConsumer<O> closeListener;

  /**
   * Constructs a new listenable output stream using the given stream as the target and the given close listener to call
   * when the stream gets closed.
   *
   * @param wrapped       the stream to wrap and delegate all write calls to.
   * @param closeListener the close listener to call when this stream gets closed.
   * @throws NullPointerException if the given wrapped stream or close listener is null.
   */
  public ListenableOutputStream(@NonNull O wrapped, @NonNull CheckedConsumer<O> closeListener) {
    this.wrapped = wrapped;
    this.closeListener = closeListener;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(int b) throws IOException {
    this.wrapped.write(b);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(byte @NonNull [] b) throws IOException {
    this.wrapped.write(b);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(byte @NonNull [] b, int off, int len) throws IOException {
    this.wrapped.write(b, off, len);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() throws IOException {
    this.wrapped.flush();
  }

  /**
   * Calls the given close listener and then closes the wrapped output stream, rethrowing any exception that was thrown
   * by either of these two actions.
   */
  @Override
  public void close() throws IOException {
    Try.of(() -> {
      this.closeListener.accept(this.wrapped);
      this.wrapped.close();
      return null;
    }).get();
  }
}
