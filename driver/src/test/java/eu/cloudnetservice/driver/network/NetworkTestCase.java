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

package eu.cloudnetservice.driver.network;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;

public abstract class NetworkTestCase {

  protected int randomFreePort(int... disabledPorts) {
    Arrays.sort(disabledPorts); // needed to make the binary search later

    var port = 1024; // first non restricted (to root user) port
    while (true) {
      // check for out of range port
      if (port > 65535) {
        Assertions.fail("No free port found in range 1024 to 65535 which causes tests to break");
      }
      // check if the port is ignored
      if (Arrays.binarySearch(disabledPorts, port) >= 0) {
        port++;
        continue;
      }
      // check if the port is in use
      if (this.isPortInUse(port)) {
        port++;
        continue;
      }
      // port is free
      break;
    }
    return port;
  }

  protected boolean isPortInUse(int port) {
    try (var ignored = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
      return false;
    } catch (Exception ignored) {
      return true;
    }
  }
}
