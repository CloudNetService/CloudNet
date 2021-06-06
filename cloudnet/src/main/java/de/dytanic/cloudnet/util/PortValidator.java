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

package de.dytanic.cloudnet.util;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public final class PortValidator {

  public static boolean checkPort(int port) {
    try (ServerSocket serverSocket = new ServerSocket()) {
      serverSocket.bind(new InetSocketAddress(port));
      return true;
    } catch (Exception exception) {
      return false;
    }
  }

  public static boolean checkHost(String host, int port) {
    try (ServerSocket serverSocket = new ServerSocket()) {
      serverSocket.bind(new InetSocketAddress(host, port));
      return true;
    } catch (Exception exception) {
      return false;
    }
  }

  public static boolean canAssignAddress(String host) {
    try (ServerSocket serverSocket = new ServerSocket()) {
      serverSocket.bind(new InetSocketAddress(host, 45893));
      return true;
    } catch (Exception exception) {
      return exception instanceof BindException
        && exception.getMessage() != null
        && exception.getMessage().startsWith("Address already in use");
    }
  }

  public static int findFreePort(int startPort) {
    while (!checkPort(startPort)) {
      ++startPort;
    }
    return startPort;
  }

}
