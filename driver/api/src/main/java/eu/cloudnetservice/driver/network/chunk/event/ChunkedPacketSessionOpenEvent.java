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

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.network.chunk.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketHandler;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a chunked packet transfer to the current network component was requested and is about to start. This
 * event is used in combination with the event chunk handler factory which is calling this event to determine which
 * handler should be used for the transfer. If no handler gets set by any listener this results in an exception and
 * prevents the chunk handling transfer from happening. If the event based solution is not used as the factory, this
 * event will (by default) never get fired.
 *
 * @since 4.0
 */
public final class ChunkedPacketSessionOpenEvent extends Event {

  private final ChunkSessionInformation sessionInformation;
  private volatile ChunkedPacketHandler chunkedPacketHandler;

  /**
   * Constructs a new chunked packet session open event with the given session information as the reason for it to get
   * fired.
   *
   * @param sessionInformation the chunked transfer session information.
   * @throws NullPointerException if the given session information is null.
   */
  public ChunkedPacketSessionOpenEvent(@NonNull ChunkSessionInformation sessionInformation) {
    this.sessionInformation = sessionInformation;
  }

  /**
   * Get the session information about the chunked transfer that was requested. Further data will only contain some
   * information to identify itself.
   *
   * @return the session information about the chunked transfer that was requested.
   */
  public @NonNull ChunkSessionInformation session() {
    return this.sessionInformation;
  }

  /**
   * Get the handler which should be used to open the session. If no handler is defined then the transfer will not
   * happen and an exception will be thrown to indicate that the handler is missing.
   *
   * @return the handler which should be used to open the session.
   */
  public @Nullable ChunkedPacketHandler handler() {
    return this.chunkedPacketHandler;
  }

  /**
   * Sets the handler which should be used to open session. Null is an accepted value and will result in the factory to
   * not be set. If the factory is not set after all listeners were called, the transfer of the chunked data will not
   * happen and an exception will be thrown to indicate that.
   *
   * @param chunkedPacketHandler the handler for the session to use.
   */
  public void handler(@Nullable ChunkedPacketHandler chunkedPacketHandler) {
    this.chunkedPacketHandler = chunkedPacketHandler;
  }
}
