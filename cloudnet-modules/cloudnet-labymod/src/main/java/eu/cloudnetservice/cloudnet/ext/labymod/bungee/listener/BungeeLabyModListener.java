package eu.cloudnetservice.cloudnet.ext.labymod.bungee.listener;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConfiguration;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.player.LabyModPlayerOptions;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class BungeeLabyModListener implements Listener {

    @EventHandler
    public void handle(ServerConnectedEvent event) {
        ICloudPlayer cloudPlayer = BridgePlayerManager.getInstance().getOnlinePlayer(event.getPlayer().getUniqueId());
        if (cloudPlayer != null && LabyModUtils.getLabyModOptions(cloudPlayer) != null) {
            ServiceInfoSnapshot serviceInfoSnapshot = BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.get(event.getServer().getInfo().getName());
            this.sendLabyModServerUpdate(event.getPlayer(), cloudPlayer, serviceInfoSnapshot);
        }
    }

    @EventHandler
    public void handle(PluginMessageEvent event) {
        LabyModConfiguration configuration = LabyModUtils.getConfiguration();
        if (configuration == null || !configuration.isEnabled() || !event.getTag().equals("LMC")) {
            return;
        }

        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        Pair<String, JsonDocument> pair = LabyModUtils.readLMCMessageContents(event.getData());
        String messageKey = pair.getFirst();
        JsonDocument messageContents = pair.getSecond();

        ICloudPlayer cloudPlayer = BridgePlayerManager.getInstance().getOnlinePlayer(player.getUniqueId());
        if (cloudPlayer == null) {
            return;
        }

        if (messageKey.equals("INFO")) {
            if (LabyModUtils.getLabyModOptions(cloudPlayer) != null) {
                return;
            }
            LabyModPlayerOptions labyModOptions = messageContents.toInstanceOf(LabyModPlayerOptions.class);
            if (!labyModOptions.isValid()) {
                return;
            }
            LabyModUtils.setLabyModOptions(cloudPlayer, labyModOptions);
            BridgePlayerManager.getInstance().updateOnlinePlayer(cloudPlayer);

            if (cloudPlayer.getConnectedService() != null) {
                ServiceInfoSnapshot serviceInfoSnapshot = BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.get(cloudPlayer.getConnectedService().getServerName());
                this.sendLabyModServerUpdate(player, cloudPlayer, serviceInfoSnapshot);
            }
        }

        if (messageKey.equals("discord_rpc")) {
            if (messageContents.contains("joinSecret")) {
                UUID joinSecret = messageContents.get("joinSecret", UUID.class);
                if (joinSecret == null) {
                    return;
                }

                BridgePlayerManager.getInstance().getOnlinePlayersAsync().onComplete(onlinePlayers -> onlinePlayers.stream()
                        .filter(o -> LabyModUtils.getLabyModOptions(o) != null)
                        .filter(o -> LabyModUtils.getLabyModOptions(o).getJoinSecret() != null)
                        .filter(o -> LabyModUtils.getLabyModOptions(o).getJoinSecret().equals(joinSecret))
                        .findFirst()
                        .ifPresent(secretOwner -> {
                            if (secretOwner.getConnectedService() == null) {
                                return;
                            }
                            LabyModPlayerOptions options = LabyModUtils.getLabyModOptions(secretOwner);
                            if (options.getLastJoinSecretRedeem() != -1 &&
                                    options.getLastJoinSecretRedeem() + 1000 > System.currentTimeMillis()) {
                                return;
                            }
                            options.setLastJoinSecretRedeem(System.currentTimeMillis());

                            ServiceInfoSnapshot connectedService = BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.get(secretOwner.getConnectedService().getServerName());

                            byte[] discordRPCData = LabyModUtils.getDiscordRPCGameInfoUpdateMessageContents(cloudPlayer, connectedService);
                            if (discordRPCData != null) {
                                BridgePlayerManager.getInstance().proxySendPluginMessage(secretOwner, LabyModConstants.LMC_CHANNEL_NAME, discordRPCData);
                            }

                            player.connect(ProxyServer.getInstance().getServerInfo(secretOwner.getConnectedService().getServerName()));

                            BridgePlayerManager.getInstance().updateOnlinePlayer(secretOwner);
                        }));
            }
        }
    }

    private void sendLabyModServerUpdate(ProxiedPlayer player, ICloudPlayer cloudPlayer, ServiceInfoSnapshot serviceInfoSnapshot) {
        byte[] gameModeData = LabyModUtils.getShowGameModeMessageContents(serviceInfoSnapshot);
        byte[] discordRPCData = LabyModUtils.getDiscordRPCGameInfoUpdateMessageContents(cloudPlayer, serviceInfoSnapshot);
        if (gameModeData != null) {
            player.sendData(LabyModConstants.LMC_CHANNEL_NAME, gameModeData);
        }
        if (discordRPCData != null) {
            player.sendData(LabyModConstants.LMC_CHANNEL_NAME, discordRPCData);
        }
    }

}
