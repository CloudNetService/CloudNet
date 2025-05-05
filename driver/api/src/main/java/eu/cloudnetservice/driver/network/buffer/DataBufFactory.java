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

package eu.cloudnetservice.driver.network.buffer;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import lombok.NonNull;

/**
 * A factory for creation of data buf instances in various forms.
 *
 * @since 4.0
 */
public interface DataBufFactory {

  /**
   * Get the default factory for data buffers integrated into cloudnet. This is current a factory which wraps netty byte
   * buffers.
   * <p>
   * Returned buffers created by this factory should <strong>NEVER</strong> get cast to any implementation of a buffer
   * as the factory might change and return different buffers when we should ever decide to use another networking
   * library for CloudNet.
   *
   * @return the default integrated data buf factory.
   */
  static @NonNull DataBufFactory defaultFactory() {
    return ServiceRegistry.registry().defaultInstance(DataBufFactory.class);
  }

  /**
   * Creates an empty buffer which can be expanded by writing to it.
   *
   * @return a new, empty data buf.
   */
  @NonNull
  DataBuf.Mutable createEmpty();

  /**
   * Creates a new readonly buffer wrapping the given byte array and using it as it's data source. Modification to the
   * given bytes will be visible in the buffer.
   *
   * @param bytes the bytes to wrap.
   * @return a new buffer wrapping the given bytes.
   */
  @NonNull
  DataBuf fromBytes(byte[] bytes);

  /**
   * Creates a readonly copy of the given data buffer. The copied variant of the buffer will start the read process from
   * the first byte rather than re-using the current index of the original buffer.
   * <p>
   * A factory is only expected to be able to copy a buffer created by it. Cross factory copy might be possible but is
   * not a requirement. Use {@code factory.createOf(buffer.toByteArray)} if you want to be sure that the buffer can be
   * copied.
   *
   * @param dataBuf the buffer to copy.
   * @return a copied variant of the given buffer.
   * @throws IllegalArgumentException if the buffer cannot be copied.
   * @throws NullPointerException     if the given buffer is null.
   */
  @NonNull
  DataBuf copyOf(@NonNull DataBuf dataBuf);

  /**
   * Creates a mutable copy of the given data buffer. The copied variant of the buffer will start the read and write
   * process from the first byte rather than re-using the current index of the original buffer.
   * <p>
   * A factory is only expected to be able to copy a buffer created by it. Cross factory copy might be possible but is
   * not a requirement. Use {@code factory.createOf(buffer.toByteArray)} if you want to be sure that the buffer can be
   * copied.
   *
   * @param dataBuf the buffer to copy.
   * @return a copied variant of the given buffer.
   * @throws IllegalArgumentException if the buffer cannot be copied.
   * @throws NullPointerException     if the given buffer is null.
   */
  @NonNull
  DataBuf.Mutable mutableCopyOf(@NonNull DataBuf dataBuf);

  /**
   * Creates an empty mutable data buffer which pre-allocates the specified amount of expected bytes rather than
   * dynamically growing during write operations.
   *
   * @param byteSize the expected amount of bytes which get written.
   * @return a new, mutable buffer with the given amount of bytes pre-allocated.
   */
  @NonNull
  DataBuf.Mutable createWithExpectedSize(int byteSize);
}
