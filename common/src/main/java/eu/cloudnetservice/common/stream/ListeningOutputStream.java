/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.common.stream;

import eu.cloudnetservice.common.function.ThrowableConsumer;
import java.io.IOException;
import java.io.OutputStream;
import lombok.NonNull;

public final class ListeningOutputStream<O extends OutputStream> extends OutputStream {

  private final O wrapped;
  private final ThrowableConsumer<O, IOException> closeListener;

  public ListeningOutputStream(O wrapped, ThrowableConsumer<O, IOException> closeListener) {
    this.wrapped = wrapped;
    this.closeListener = closeListener;
  }

  @Override
  public void write(int b) throws IOException {
    this.wrapped.write(b);
  }

  @Override
  public void write(byte @NonNull [] b) throws IOException {
    this.wrapped.write(b);
  }

  @Override
  public void write(byte @NonNull [] b, int off, int len) throws IOException {
    this.wrapped.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    this.wrapped.flush();
  }

  @Override
  public void close() throws IOException {
    this.closeListener.accept(this.wrapped);
    this.wrapped.close();
  }
}
