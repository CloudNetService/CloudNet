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

package de.dytanic.cloudnet.driver.event.events.chunk;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketHandler;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ChunkedPacketSessionOpenEvent extends Event {

  private final ChunkSessionInformation sessionInformation;
  private volatile ChunkedPacketHandler chunkedPacketHandler;

  public ChunkedPacketSessionOpenEvent(@NotNull ChunkSessionInformation sessionInformation) {
    this.sessionInformation = sessionInformation;
  }

  public @NotNull ChunkSessionInformation session() {
    return this.sessionInformation;
  }

  public @Nullable ChunkedPacketHandler handler() {
    return this.chunkedPacketHandler;
  }

  public void handler(@Nullable ChunkedPacketHandler chunkedPacketHandler) {
    this.chunkedPacketHandler = chunkedPacketHandler;
  }
}
