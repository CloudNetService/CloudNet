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
