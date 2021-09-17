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

package de.dytanic.cloudnet.driver.network.netty.buffer;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

public class NettyDataBufFactory implements DataBufFactory {

  public static final NettyDataBufFactory INSTANCE = new NettyDataBufFactory();

  protected NettyDataBufFactory() {
  }

  @Override
  public @NotNull DataBuf.Mutable createEmpty() {
    return new NettyMutableDataBuf(Unpooled.buffer());
  }

  @Override
  public @NotNull DataBuf createReadOnly() {
    return new NettyImmutableDataBuf(Unpooled.EMPTY_BUFFER);
  }

  @Override
  public @NotNull DataBuf copyOf(@NotNull DataBuf dataBuf) {
    Verify.verify(dataBuf instanceof NettyImmutableDataBuf, "Factory only supports netty data buf copy");
    return new NettyImmutableDataBuf(Unpooled.copiedBuffer(((NettyImmutableDataBuf) dataBuf).byteBuf));
  }

  @Override
  public @NotNull DataBuf.Mutable mutableCopyOf(@NotNull DataBuf dataBuf) {
    Verify.verify(dataBuf instanceof NettyImmutableDataBuf, "Factory only supports netty data buf copy");
    return new NettyMutableDataBuf(Unpooled.copiedBuffer(((NettyImmutableDataBuf) dataBuf).byteBuf));
  }

  @Override
  public @NotNull DataBuf.Mutable createWithExpectedSize(int byteSize) {
    return new NettyMutableDataBuf(Unpooled.buffer(byteSize));
  }
}
