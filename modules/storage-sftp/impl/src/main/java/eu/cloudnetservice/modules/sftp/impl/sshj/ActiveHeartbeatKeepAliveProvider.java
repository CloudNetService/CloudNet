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

package eu.cloudnetservice.modules.sftp.impl.sshj;

import lombok.NonNull;
import net.schmizz.keepalive.KeepAlive;
import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.connection.ConnectionImpl;

public final class ActiveHeartbeatKeepAliveProvider extends KeepAliveProvider {

  private static final int HEARTBEAT_DELAY_SECONDS = 15;
  public static KeepAliveProvider INSTANCE = new ActiveHeartbeatKeepAliveProvider();

  @Override
  public @NonNull KeepAlive provide(@NonNull ConnectionImpl connection) {
    var keepAlive = KeepAliveProvider.HEARTBEAT.provide(connection);
    keepAlive.setKeepAliveInterval(HEARTBEAT_DELAY_SECONDS);
    return keepAlive;
  }
}
