package de.dytanic.cloudnet.driver.network.netty;
/*
 * Created by derrop on 31.08.2019
 */

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Random;

public class NettyTestUtil {

    public static int generateRandomPort(int min, int max) {
        Random random = new Random();
        int port;
        do {
            port = random.nextInt(max - min) + min;
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
        } catch (Exception e) {
        }
        return false;
    }

}
