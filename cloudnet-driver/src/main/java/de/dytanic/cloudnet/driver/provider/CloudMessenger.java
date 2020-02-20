package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import org.jetbrains.annotations.NotNull;

public interface CloudMessenger {

    void sendChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

    void sendChannelMessage(@NotNull ServiceInfoSnapshot targetServiceInfoSnapshot, @NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

    void sendChannelMessage(@NotNull ServiceTask targetServiceTask, @NotNull String channel, @NotNull String message, @NotNull JsonDocument data);
    
}
