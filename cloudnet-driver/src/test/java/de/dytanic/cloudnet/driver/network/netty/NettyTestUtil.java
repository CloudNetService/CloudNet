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
