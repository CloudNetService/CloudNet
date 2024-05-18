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

package eu.cloudnetservice.driver.network.netty.buffer;

import com.google.common.base.Preconditions;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import io.netty5.buffer.BufferAllocator;
import io.netty5.buffer.DefaultBufferAllocators;
import jakarta.inject.Singleton;
import lombok.NonNull;

/**
 * An implementation (and currently the default one) of a data buf factory wrapping netty byte buffers.
 *
 * @see DataBufFactory#defaultFactory()
 * @since 4.0
 */
@Singleton
@Provides(DataBufFactory.class)
public class NettyDataBufFactory implements DataBufFactory {

  public static final NettyDataBufFactory INSTANCE = new NettyDataBufFactory();
  // we always use off-heap as this is the preferred allocator on Java 9+ (and we required Java 17)
  protected static final BufferAllocator ALLOCATOR = DefaultBufferAllocators.offHeapAllocator();

  /**
   * Creates a new instance of this factory. This method is protected to allow developers to create their own variant of
   * this factory. In normal cases {@link DataBufFactory#defaultFactory()} should be used to obtain the instance of this
   * factory.
   */
  protected NettyDataBufFactory() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable createEmpty() {
    return new NettyMutableDataBuf(ALLOCATOR.allocate(0));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf fromBytes(byte[] bytes) {
    return new NettyImmutableDataBuf(ALLOCATOR.copyOf(bytes));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf copyOf(@NonNull DataBuf dataBuf) {
    Preconditions.checkArgument(dataBuf instanceof NettyImmutableDataBuf, "Factory only supports netty data buf copy");

    // create a full copy of the buffer
    var buffer = ((NettyImmutableDataBuf) dataBuf).buffer();
    return new NettyImmutableDataBuf(buffer.copy(0, buffer.readableBytes(), true));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable mutableCopyOf(@NonNull DataBuf dataBuf) {
    Preconditions.checkArgument(dataBuf instanceof NettyImmutableDataBuf, "Factory only supports netty data buf copy");

    // create a full copy of the buffer
    var buffer = ((NettyImmutableDataBuf) dataBuf).buffer();
    return new NettyMutableDataBuf(buffer.copy(0, buffer.readableBytes()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable createWithExpectedSize(int byteSize) {
    return new NettyMutableDataBuf(ALLOCATOR.allocate(byteSize));
  }
}
