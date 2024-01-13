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

package eu.cloudnetservice.driver.network.netty.http;

import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.timeout.IdleState;
import io.netty5.handler.timeout.IdleStateEvent;
import io.netty5.handler.timeout.IdleStateHandler;
import io.netty5.handler.timeout.ReadTimeoutException;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

/**
 * A custom idle state handler which closes a connection after the given seconds of client inactivity while also
 * considering outbound events. When a http handler sends data to the client the connection will not be closed and kept
 * open until the outbound write finished and the data flushed. This is also the main difference from the default
 * ReadTimeoutHandler directly provided by netty.
 *
 * @since 4.0
 */
final class NettyIdleStateHandler extends IdleStateHandler {

  private boolean closed = false;

  /**
   * Constructs a new netty idle state handler instance.
   *
   * @param timeoutSeconds the seconds a client is allowed to idle before a forced disconnect.
   */
  public NettyIdleStateHandler(int timeoutSeconds) {
    super(0, 0, timeoutSeconds, TimeUnit.SECONDS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void channelIdle(@NonNull ChannelHandlerContext ctx, @NonNull IdleStateEvent evt) {
    if (!this.closed && evt.state() == IdleState.ALL_IDLE) {
      ctx.fireChannelExceptionCaught(ReadTimeoutException.INSTANCE);
      ctx.close();
      this.closed = true;
    }
  }
}
