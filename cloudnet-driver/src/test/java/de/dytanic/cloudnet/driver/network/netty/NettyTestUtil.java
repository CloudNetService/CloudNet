/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.netty;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Random;

public class NettyTestUtil {

  private static final Random RANDOM = new Random();

  public static int generateRandomPort(int min, int max) {
    int port;
    do {
      port = RANDOM.nextInt(max - min) + min;
    } while (!isPortAvailable(port));
    return port;
  }

  public static int generateRandomPort() {
    return generateRandomPort(10000, 50000);
  }

  public static boolean isPortAvailable(int port) {
    try (ServerSocket serverSocket = new ServerSocket()) {
      serverSocket.bind(new InetSocketAddress(port));
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

}
