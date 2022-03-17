/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.network.netty.buffer;

import com.google.common.base.VerifyException;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.cloudnet.driver.network.netty.NettyUtil;
import lombok.NonNull;

/**
 * An implementation (and currently the default one) of a data buf factory wrapping netty byte buffers.
 *
 * @see DataBufFactory#defaultFactory()
 * @since 4.0
 */
public class NettyDataBufFactory implements DataBufFactory {

  public static final NettyDataBufFactory INSTANCE = new NettyDataBufFactory();

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
    return new NettyMutableDataBuf(NettyUtil.allocator().allocate(0));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf createOf(byte @NonNull [] bytes) {
    return new NettyImmutableDataBuf(NettyUtil.allocator().copyOf(bytes));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf copyOf(@NonNull DataBuf dataBuf) {
    if (!(dataBuf instanceof NettyImmutableDataBuf buf)) {
      throw new VerifyException("Factory only supports netty data buf copy");
    }

    var buffer = buf.buffer();
    return new NettyImmutableDataBuf(buffer.copy(0, buffer.writerOffset()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable mutableCopyOf(@NonNull DataBuf dataBuf) {
    if (!(dataBuf instanceof NettyImmutableDataBuf buf)) {
      throw new VerifyException("Factory only supports netty data buf copy");
    }

    var buffer = buf.buffer();
    return new NettyMutableDataBuf(buffer.copy(0, buffer.writerOffset()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBuf.Mutable createWithExpectedSize(int byteSize) {
    return new NettyMutableDataBuf(NettyUtil.allocator().allocate(byteSize));
  }
}
