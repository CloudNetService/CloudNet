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

package eu.cloudnetservice.driver.impl.network.chunk;

import eu.cloudnetservice.driver.network.chunk.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketHandler;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.NonNull;

/**
 * A registry for chunked transfer sessions that are currently running and active.
 *
 * @since 4.0
 */
@Singleton
public final class ChunkedSessionRegistry {

  private final Map<UUID, ChunkedPacketHandler> runningSessions = new ConcurrentHashMap<>();

  /**
   * Marks the given session as completed by removing it from the lookup registry. Further tries to access a session
   * with the given unique id will create a new session instead.
   *
   * @param sessionId the id of the session to mark as completed.
   * @throws NullPointerException if the given session id is null.
   */
  public void completeSession(@NonNull UUID sessionId) {
    this.runningSessions.remove(sessionId);
  }

  /**
   * Registers the given handler for the given session id, unless another handler is already registered.
   *
   * @param sessionId the session id to register the handler for.
   * @param handler   the handler to register for the session id.
   * @throws NullPointerException if the given session id or handler is null.
   */
  public void registerSession(@NonNull UUID sessionId, @NonNull ChunkedPacketHandler handler) {
    this.runningSessions.putIfAbsent(sessionId, handler);
  }

  /**
   * Gets the currently active session or creates a new session using the given instance factory. Sessions are unique by
   * their session id. Concurrently accessing this method to create a new session will return the same session instance
   * to all callers.
   *
   * @param sessionInformation the information of the session to get or create the session of.
   * @param sessionFactory     the factory to call if no session is currently associated with the session id.
   * @return the existing registered or newly created transfer session for the provided session id.
   * @throws NullPointerException if the given session information or session factory is null.
   */
  public @NonNull ChunkedPacketHandler getOrCreateSession(
    @NonNull ChunkSessionInformation sessionInformation,
    @NonNull Function<ChunkSessionInformation, ChunkedPacketHandler> sessionFactory
  ) {
    return this.runningSessions.computeIfAbsent(
      sessionInformation.sessionUniqueId(),
      _ -> sessionFactory.apply(sessionInformation));
  }
}
