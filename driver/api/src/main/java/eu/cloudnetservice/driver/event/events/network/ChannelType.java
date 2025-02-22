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

package eu.cloudnetservice.driver.event.events.network;

/**
 * Represents a type of channel. A client channel is a channel which connects from or to a client. For instance, all
 * channels opened by a wrapper are client channels. On the other hand, a server channel represents a channel to a
 * network server. All wrapper connections to the server are handled as server channels.
 *
 * @since 4.0
 */
public enum ChannelType {

  /**
   * A channel which connects from or to a client.
   */
  CLIENT_CHANNEL,
  /**
   * All channels which are connecting to a server.
   */
  SERVER_CHANNEL
}
