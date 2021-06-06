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
