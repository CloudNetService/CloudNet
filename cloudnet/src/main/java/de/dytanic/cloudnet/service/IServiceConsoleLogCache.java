package de.dytanic.cloudnet.service;

import java.util.Queue;

public interface IServiceConsoleLogCache {

    ICloudService getCloudService();

    Queue<String> getCachedLogMessages();

    IServiceConsoleLogCache update();

    boolean isAutoPrintReceivedInput();

    void setAutoPrintReceivedInput(boolean value);

    boolean isScreenEnabled();

    void setScreenEnabled(boolean value);

}