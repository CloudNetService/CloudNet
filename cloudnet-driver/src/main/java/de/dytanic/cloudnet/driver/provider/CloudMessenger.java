package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;

public interface CloudMessenger {

    void sendChannelMessage(String channel, String message, JsonDocument data);

    void sendChannelMessage(ServiceInfoSnapshot targetServiceInfoSnapshot, String channel, String message, JsonDocument data);

    void sendChannelMessage(ServiceTask targetServiceTask, String channel, String message, JsonDocument data);
    
}
