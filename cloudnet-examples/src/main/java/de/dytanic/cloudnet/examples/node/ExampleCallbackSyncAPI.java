package de.dytanic.cloudnet.examples.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveCallablePacketEvent;

import java.util.concurrent.ExecutionException;

public final class ExampleCallbackSyncAPI {


    //Node part

    //Node event listener
    @EventListener
    public void handle(NetworkChannelReceiveCallablePacketEvent event) {
        if (!event.getChannelName().equalsIgnoreCase("test_channel")) return;

        switch (event.getId()) {
            case "get_node_count":
                event.setCallbackPacket(new JsonDocument("nodeCount", CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers().size()));
                //Set the callback packet data for the node
                break;
        }
    }

    /*= ------------------------------------------------------------- =*/
    //Plugin or Wrapper send and get

    public ITask<Integer> getNodeCountAsync() {
        //The callback information from node and the map to an Integer value
        return CloudNetDriver.getInstance().sendCallablePacket( //Send a packet, which has to get a message back async.
                CloudNetDriver.getInstance().getNetworkClient().getChannels().iterator().next(),
                "test_channel",
                "get_node_count",
                new JsonDocument(),
                jsonDocument -> jsonDocument.getInt("nodeCount")
        );
    }

    //Working with getNodeCount()

    public void workingWithGetNodeCount() {
        //Async operation
        getNodeCountAsync().addListener(new ITaskListener<Integer>() {
            @Override
            public void onComplete(ITask<Integer> task, Integer integer) {
                System.out.println("Current Node count with async callback: " + integer);
            }
        });

        //Sync Operation
        try {
            int count = getNodeCountAsync().get();
            System.out.println("Current Node count: " + count);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}