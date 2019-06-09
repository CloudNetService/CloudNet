package de.dytanic.cloudnet.util;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

public final class PortValidator {

    public static boolean checkPort(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(port));
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }
}