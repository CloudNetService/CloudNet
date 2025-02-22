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
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An event called when a request for a chunked file transmission is received to determine the handler for the chunked
 * transmission. If no handler gets set by the register event listeners, the request gets rejected.
 *
 * @since 4.0
 */
public final class FileQueryRequestEvent extends Event {

  private final String dataId;
  private final DataBuf requestData;

  private ChunkedPacketSender.Builder responseHandler;

  /**
   * Constructs a new file query request event.
   *
   * @param dataId      the id of the data that is requested by the remote.
   * @param requestData the full request data, possibly containing further information about the requested data.
   * @throws NullPointerException if the given data id or request data is null.
   */
  public FileQueryRequestEvent(@NonNull String dataId, @NonNull DataBuf requestData) {
    this.dataId = dataId;
    this.requestData = requestData;
  }

  /**
   * Get the id of the requested data.
   *
   * @return the id of the requested data.
   */
  public @NonNull String dataId() {
    return this.dataId;
  }

  /**
   * Get the full request content, possibly containing further information about the requested data.
   *
   * @return the full request content.
   */
  public @NonNull DataBuf requestData() {
    return this.requestData;
  }

  /**
   * Get the response handler builder that is responsible for responding with the requested data. Can be null if no
   * event listener set a response handler yet.
   *
   * @return the response handler builder that is responsible for responding with the requested data.
   */
  public @Nullable ChunkedPacketSender.Builder responseHandler() {
    return this.responseHandler;
  }

  /**
   * Sets the response handler to use for the request.
   *
   * @param responseHandler the response handler to use for the request.
   */
  public void responseHandler(@Nullable ChunkedPacketSender.Builder responseHandler) {
    this.responseHandler = responseHandler;
  }
}
