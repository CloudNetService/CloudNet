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
 * Holds some internal constants for network communication that are shared between wrappers and nodes.
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

  // magic packet header added to all packets to identify them as sent by CloudNet
  public static final int MAGIC_PACKET_HEADER = (0x43 << 16) | (0x4E << 8) | 0x53;

  private NetworkConstants() {
    throw new UnsupportedOperationException();
  }
}
