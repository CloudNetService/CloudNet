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
import org.jetbrains.annotations.Contract;

public record EventChunkHandlerFactory(@NonNull EventManager eventManager)
  implements Function<ChunkSessionInformation, ChunkedPacketHandler> {

  public static @NonNull EventChunkHandlerFactory withDefaultEventManager() {
    return withEventManager(CloudNetDriver.instance().eventManager());
  }

  public static @NonNull EventChunkHandlerFactory withEventManager(@NonNull EventManager manager) {
    return new EventChunkHandlerFactory(manager);
  }

  @Override
  @Contract(pure = true)
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
