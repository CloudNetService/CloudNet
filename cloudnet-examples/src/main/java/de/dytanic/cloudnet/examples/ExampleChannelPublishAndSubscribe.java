package de.dytanic.cloudnet.examples;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;

public final class ExampleChannelPublishAndSubscribe {

    public void publishGlobalMessage() {
        ChannelMessage.builder()
                .channel("user_channel")
                .message("user_info_publishing")
                .json(JsonDocument.newDocument().append("name", "Peter Parker").append("age", 17))
                .targetAll()
                .build()
                .send();
        //Send a channel message to all services and nodes in network
    }

    public void publishMessage(String targetServiceName) {
        ChannelMessage.builder()
                .channel("user_channel")
                .message("user_info_publishing")
                .json(JsonDocument.newDocument().append("name", "Peter Parker").append("age", 17))
                .targetService(targetServiceName)
                .build()
                .send();
        //Send a channel message to a specified service
    }

    @EventListener
    public void handleChannelMessage(ChannelMessageReceiveEvent event) {
        if (event.getMessage() == null || event.getData() == null) {
            return;
        }

        if (event.getChannel().equalsIgnoreCase("user_channel")) {
            if ("user_info_publishing".equals(event.getMessage().toLowerCase())) {
                System.out.println("Name: " + event.getData().getString("name") + " | Age: " + event.getData().getInt("age"));
            }
        }
        //Receive a channel message in the network
    }
}