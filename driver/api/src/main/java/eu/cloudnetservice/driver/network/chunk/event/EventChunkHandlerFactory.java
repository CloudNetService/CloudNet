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

package eu.cloudnetservice.driver.network.chunk.event;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.chunk.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketHandler;
import java.util.function.Function;
import lombok.NonNull;

/**
 * Represents a factory for chunked packet handlers using the {@code ChunkedPacketSessionOpenEvent} to determine which
 * handler to use for a chunked session.
 *
 * @since 4.0
 */
public final class EventChunkHandlerFactory implements Function<ChunkSessionInformation, ChunkedPacketHandler> {

  private final EventManager eventManager;

  /**
   * Constructs a new event chunk handler factory instance.
   *
   * @param eventManager the event manager to use to call events.
   * @throws NullPointerException if the given event manager is null.
   */
  public EventChunkHandlerFactory(@NonNull EventManager eventManager) {
    this.eventManager = eventManager;
  }

  /**
   * Get a new handler for the given session information based on the event call result (and therefore the result a
   * listener of the {@code ChunkedPacketSessionOpenEvent} set).
   *
   * @param info the session info to get the handler for.
   * @return the packet handler for the session set by the last listener in the chain.
   * @throws IllegalStateException if no listener in the chain set a handler.
   * @throws NullPointerException  if the given session info is null.
   */
  @Override
  public @NonNull ChunkedPacketHandler apply(@NonNull ChunkSessionInformation info) {
    var handler = this.eventManager.callEvent(new ChunkedPacketSessionOpenEvent(info)).handler();
    if (handler == null) {
      throw new IllegalStateException("No chunked handler for " + info);
    }

    return handler;
  }
}
