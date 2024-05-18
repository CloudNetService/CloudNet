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

package eu.cloudnetservice.modules.sftp.sshj;

import lombok.NonNull;
import net.schmizz.keepalive.KeepAlive;
import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.common.Message;
import net.schmizz.sshj.common.SSHPacket;
import net.schmizz.sshj.connection.ConnectionImpl;
import net.schmizz.sshj.transport.TransportException;

public final class ActiveHeartbeatKeepAliveProvider extends KeepAliveProvider {

  private static final int HEARTBEAT_DELAY_SECONDS = 15;
  public static KeepAliveProvider INSTANCE = new ActiveHeartbeatKeepAliveProvider();

  @Override
  public @NonNull KeepAlive provide(@NonNull ConnectionImpl connection) {
    var keepAlive = new HeartbeatKeepAlive(connection);
    keepAlive.setKeepAliveInterval(HEARTBEAT_DELAY_SECONDS);
    return keepAlive;
  }

  private static final class HeartbeatKeepAlive extends KeepAlive {

    public HeartbeatKeepAlive(@NonNull ConnectionImpl conn) {
      super(conn, "cloudnet-ssh-heartbeater");
    }

    @Override
    protected void doKeepAlive() throws TransportException {
      // when the server wants a strict key exchange, no other packets are allowed
      // to be sent in that time interval (KEX_INIT must be the first packet). As it's
      // very unlikely that a heartbeat is required during the timeframe anyway, we just
      // don't execute the heartbeat until the key exchange is completed.
      // Done by ensuring that the service is set. This means that the key
      // exchange is done and the connection is up.
      var transport = this.conn.getTransport();
      if (this.conn.equals(transport.getService())) {
        transport.write(new SSHPacket(Message.IGNORE));
      }
    }
  }
}
