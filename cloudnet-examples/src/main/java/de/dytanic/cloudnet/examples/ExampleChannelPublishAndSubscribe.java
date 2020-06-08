package de.dytanic.cloudnet.examples;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

import java.util.UUID;

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

    public String sendQueryToService(String targetServiceName) {
        //Send a channel message to a specified service and get a response

        ChannelMessage response = ChannelMessage.builder()
                .channel("user_channel")
                .message("user_info")
                .json(JsonDocument.newDocument().append("any request", "..."))
                .targetService(targetServiceName)
                .build()
                .sendSingleQuery();

        if (response == null) {
            return null;
        }

        String binaryResponse = response.getBuffer().readString();
        String jsonResponse = response.getJson().getString("test");
        JsonDocument requested = response.getJson().getDocument("requested");

        return jsonResponse;
    }

    @EventListener
    public void handleChannelMessage(ChannelMessageReceiveEvent event) {
        if (event.getMessage() == null) {
            return;
        }

        if (event.getChannel().equalsIgnoreCase("user_channel")) {
            if ("user_info_publishing".equals(event.getMessage().toLowerCase())) {
                System.out.println("Name: " + event.getData().getString("name") + " | Age: " + event.getData().getInt("age"));
            }
        }
        //Receive a channel message in the network
    }

    @EventListener
    public void handleQueryChannelMessage(ChannelMessageReceiveEvent event) {
        if (event.getMessage() == null || !event.isQuery()) {
            return;
        }

        if (event.getChannel().equalsIgnoreCase("user_channel")) {
            if ("user_info".equals(event.getMessage().toLowerCase())) {
                event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                        .json(JsonDocument.newDocument("test", "response string").append("requested", event.getData()))
                        .buffer(ProtocolBuffer.create().writeString("binary response"))
                        .build());
            }
        }
        //Receive a channel message in the network
    }

}