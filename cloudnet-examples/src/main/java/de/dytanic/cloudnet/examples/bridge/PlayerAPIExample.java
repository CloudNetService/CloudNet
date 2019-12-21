package de.dytanic.cloudnet.examples.bridge;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoSnapshotUtil;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudPlayerEvent;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public final class PlayerAPIExample {

    //Returns the player online count from a task synchronized
    public int countServiceInfoSnapshotPlayerCount() {
        int counter = 0;

        for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices("Lobby")) {
            counter += ServiceInfoSnapshotUtil.getOnlineCount(serviceInfoSnapshot);
        }

        return counter;
    }

    //Asynchronous variant of count the player count from a task
    public void countServiceInfoSnapshotPlayerCount(Consumer<Integer> consumer) {
        CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesAsync("Lobby").onComplete(serviceInfoSnapshots -> {
            int counter = 0;

            if (serviceInfoSnapshots != null) {
                for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices("Lobby")) {
                    counter += ServiceInfoSnapshotUtil.getOnlineCount(serviceInfoSnapshot);
                }
            }

            consumer.accept(counter);
        });
    }

    public void getPlayerExample(Player player) //Bukkit Player
    {
        Validate.checkNotNull(player);

        ICloudPlayer cloudPlayer = BridgePlayerManager.getInstance().getOnlinePlayer(player.getUniqueId()); //Returns an online cloudPlayers

        if (cloudPlayer != null) //Checks that the player is online
        {
            cloudPlayer.getUniqueId(); //Returns the uniqueId
            cloudPlayer.getName(); //Returns the players name
            cloudPlayer.getXBoxId(); //Bedrock Edition XBoxId
        }

        ICloudOfflinePlayer cloudOfflinePlayer = BridgePlayerManager.getInstance().getOfflinePlayer(player.getUniqueId());
        //Returns the cloud offline player with all players data from the database


        if (cloudOfflinePlayer != null) //Checks the cloud offline player if it registered on CloudNet
        {
            cloudOfflinePlayer.getFirstLoginTimeMillis(); //First login in milliseconds since 1.1.1970
            cloudOfflinePlayer.getLastLoginTimeMillis(); //Last login or the current login in milliseconds
        }


        List<? extends ICloudPlayer> cloudPlayers = BridgePlayerManager.getInstance().getOnlinePlayers(player.getName());

        if (cloudPlayers != null && !cloudPlayers.isEmpty()) //If player instances with that name is contain
        {
            ICloudPlayer entry = cloudPlayers.get(0);

            BridgePlayerManager.getInstance().proxyKickPlayer(entry, "Kick message"); //kicks a player from the network
            BridgePlayerManager.getInstance().proxySendPlayer(entry, "Lobby-3"); //send a player to the target server if the player is login on a proxy
            BridgePlayerManager.getInstance().proxySendPlayerMessage(entry, "Hello, player!"); //send the player a text message
        }
    }

    //Add additional properties
    public void addPropertiesToPlayer(Player player) //Bukkit org.bukkit.entity.Player
    {
        ICloudPlayer cloudPlayer = BridgePlayerManager.getInstance().getOnlinePlayer(player.getUniqueId());

        if (cloudPlayer != null) {
            cloudPlayer.getProperties()
                    .append("my custom property", 42)
            ;

            BridgePlayerManager.getInstance().updateOnlinePlayer(cloudPlayer);
        }
    }

    //Handles an online player update only
    @EventListener
    public void handle(BridgeUpdateCloudPlayerEvent event) {
        ICloudPlayer cloudPlayer = event.getCloudPlayer();

        int myCustomProperty = cloudPlayer.getProperties().getInt("my custom property"); //42
    }

    //Handles an offline player update only
    @EventListener
    public void handle(BridgeUpdateCloudOfflinePlayerEvent event) {
        ICloudOfflinePlayer cloudPlayer = event.getCloudOfflinePlayer();

        /* ... */ //do something
    }
}