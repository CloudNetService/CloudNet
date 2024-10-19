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

package eu.cloudnetservice.driver.impl.network;

/**
 * Holds some internal constants for network communication which are shared between wrappers and nodes. The class is
 * marked as internal, however developers are allowed to use this class. <strong>BUT</strong> there might be changes to
 * this class which are breaking, even when doing a patch release (for example a constant can get removed or changed).
 * This class should therefore be used with caution.
 *
 * @since 4.0
 */
public final class NetworkConstants {

  // reserved internal packet ids
  public static final int INTERNAL_RPC_COM_CHANNEL = 0;
  public static final int CHANNEL_MESSAGING_CHANNEL = 1;
  public static final int CHUNKED_PACKET_COM_CHANNEL = 2;
  public static final int INTERNAL_AUTHORIZATION_CHANNEL = 3;
  public static final int INTERNAL_SERVICE_SYNC_ACK_CHANNEL = 4;

  // channel message channels
  public static final String INTERNAL_MSG_CHANNEL = "cloudnet:internal";

  private NetworkConstants() {
    throw new UnsupportedOperationException();
  }
}
