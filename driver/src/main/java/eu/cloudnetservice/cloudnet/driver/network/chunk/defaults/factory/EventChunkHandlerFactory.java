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

package eu.cloudnetservice.cloudnet.driver.network.chunk.defaults.factory;

import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.event.events.chunk.ChunkedPacketSessionOpenEvent;
import eu.cloudnetservice.cloudnet.driver.network.chunk.ChunkedPacketHandler;
import eu.cloudnetservice.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import java.util.function.Function;
import lombok.NonNull;

/**
 * Represents a factory for chunked packet handlers using the {@code ChunkedPacketSessionOpenEvent} to determine which
 * handler to use for a chunked session.
 *
 * @since 4.0
 */
public class EventChunkHandlerFactory implements Function<ChunkSessionInformation, ChunkedPacketHandler> {

  private final EventManager eventManager;

  /**
   * Constructs a new event chunk handler factory instance. This constructor should only be used when extending from the
   * class, see {@link #withEventManager(EventManager)} and {@link #withDefaultEventManager()} instead.
   *
   * @param eventManager the event manager to use to call events.
   * @throws NullPointerException if the given event manager is null.
   */
  protected EventChunkHandlerFactory(@NonNull EventManager eventManager) {
    this.eventManager = eventManager;
  }

  /**
   * Constructs a new instance of this factory using the default event manager provided by the driver.
   *
   * @return a new factory using the default system event manager.
   */
  public static @NonNull EventChunkHandlerFactory withDefaultEventManager() {
    return withEventManager(CloudNetDriver.instance().eventManager());
  }

  /**
   * Constructs a new instance of this factory using the given event manager. The given event manager will be used to
   * call the event, therefore the event manager must also hold the listeners which are responsible to set the factory.
   *
   * @param manager the event manager to use for this factory.
   * @return a new factory using the given event manager.
   * @throws NullPointerException if the given event manager is null.
   */
  public static @NonNull EventChunkHandlerFactory withEventManager(@NonNull EventManager manager) {
    return new EventChunkHandlerFactory(manager);
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
    // get the chunked packet handler for the session
    var handler = this.eventManager.callEvent(new ChunkedPacketSessionOpenEvent(info)).handler();
    // check if there was a handler supplied
    if (handler == null) {
      throw new IllegalStateException("No chunked handler for " + info);
    }
    // return the created handler
    return handler;
  }
}
