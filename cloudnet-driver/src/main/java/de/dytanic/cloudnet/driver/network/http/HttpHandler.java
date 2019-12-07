package de.dytanic.cloudnet.driver.network.http;

public interface HttpHandler {

    int PRIORITY_HIGH = 64, PRIORITY_NORMAL = 32, PRIORITY_LOW = 16, PRIORITY_LOWEST = 0;

    void handle(String path, HttpContext context) throws Exception;

}