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

package de.dytanic.cloudnet.driver.network.protocol.chunk.listener;

import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedQueryResponse;
import java.io.InputStream;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class ConsumingChunkedPacketListener extends CachedChunkedPacketListener {

  private final Consumer<ChunkedQueryResponse> consumer;

  public ConsumingChunkedPacketListener(Consumer<ChunkedQueryResponse> consumer) {
    this.consumer = consumer;
  }

  @Override
  protected void handleComplete(@NotNull ChunkedPacketSession session, @NotNull InputStream inputStream) {
    this.consumer
      .accept(new ChunkedQueryResponse(session, session.getFirstPacket(), session.getLastPacket(), inputStream));
  }
}
