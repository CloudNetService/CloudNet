package de.dytanic.cloudnet.examples;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;

public final class ExampleChannelPublishAndSubscribe {

    public void publishMessage() {
        CloudNetDriver.getInstance().sendChannelMessage("user_channel", "user_info_publishing", new JsonDocument()
                .append("name", "Peter Parker")
                .append("age", 17)
        );
        //Send a channel message to all services and nodes in network
    }

    @EventListener
    public void handleChannelMessage(ChannelMessageReceiveEvent event) {
        if (event.getChannel().equalsIgnoreCase("user_channel")) {
            switch (event.getMessage().toLowerCase()) {
                case "user_info_publishing":

                    System.out.println("Name: " + event.getData().getString("name") + " | Age: " + event.getData().getInt("age"));
                    break;
            }
        }
        //Receive a channel message in the network
    }
}