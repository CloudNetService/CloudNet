package eu.cloudnetservice.cloudnet.ext.labymod.bungee.listener;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.EventPriority;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModChannelUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
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
            ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper.getCachedServiceInfoSnapshot(event.getServer().getInfo().getName());
            this.sendLabyModServerUpdate(event.getPlayer(), cloudPlayer, serviceInfoSnapshot);
        }
    }

    @EventListener(priority = EventPriority.HIGHEST)
    public void handle(CloudServiceInfoUpdateEvent event) {
        ServiceInfoSnapshot newServiceInfoSnapshot = event.getServiceInfo();
        ServiceInfoSnapshot oldServiceInfoSnapshot = BridgeProxyHelper.getCachedServiceInfoSnapshot(newServiceInfoSnapshot.getServiceId().getName());
        if (oldServiceInfoSnapshot == null) {
            return;
        }

        if (LabyModUtils.canSpectate(newServiceInfoSnapshot) &&
                !oldServiceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false)) {
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                if (player.getServer() == null || player.getServer().getInfo() == null) {
                    continue;
                }
                if (!player.getServer().getInfo().getName().equals(newServiceInfoSnapshot.getName())) {
                    continue;
                }

                ICloudPlayer cloudPlayer = BridgePlayerManager.getInstance().getOnlinePlayer(player.getUniqueId());
                if (cloudPlayer != null && LabyModUtils.getLabyModOptions(cloudPlayer) != null) {
                    this.sendLabyModServerUpdate(player, cloudPlayer, newServiceInfoSnapshot);
                }
            }
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

        Pair<String, JsonDocument> pair = LabyModChannelUtils.readLMCMessageContents(event.getData());
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
            labyModOptions.setCreationTime(System.currentTimeMillis());
            LabyModUtils.setLabyModOptions(cloudPlayer, labyModOptions);
            BridgePlayerManager.getInstance().updateOnlinePlayer(cloudPlayer);

            if (cloudPlayer.getConnectedService() != null) {
                ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper.getCachedServiceInfoSnapshot(cloudPlayer.getConnectedService().getServerName());
                this.sendLabyModServerUpdate(player, cloudPlayer, serviceInfoSnapshot);
            }
        }

        if (messageKey.equals("discord_rpc")) {
            if (messageContents.contains("joinSecret")) {
                UUID joinSecret = messageContents.get("joinSecret", UUID.class);
                if (joinSecret == null) {
                    return;
                }

                LabyModUtils.getPlayerByJoinSecret(joinSecret).onComplete(secretOwner -> {
                    if (secretOwner == null || secretOwner.getConnectedService() == null) {
                        return;
                    }
                    LabyModPlayerOptions options = LabyModUtils.getLabyModOptions(secretOwner);
                    if (options == null || (options.getLastJoinSecretRedeem() != -1 &&
                            options.getLastJoinSecretRedeem() + 1000 > System.currentTimeMillis())) {
                        return;
                    }
                    options.setLastJoinSecretRedeem(System.currentTimeMillis());

                    this.connectTo(player, secretOwner);
                });
            }

            if (messageContents.contains("spectateSecret")) {
                UUID spectateSecret = messageContents.get("spectateSecret", UUID.class);
                if (spectateSecret == null) {
                    return;
                }

                LabyModUtils.getPlayerBySpectateSecret(spectateSecret).onComplete(secretOwner -> {
                    if (secretOwner == null || secretOwner.getConnectedService() == null) {
                        return;
                    }
                    LabyModPlayerOptions options = LabyModUtils.getLabyModOptions(secretOwner);
                    if (options == null || (options.getLastSpectateSecretRedeem() != -1 &&
                            options.getLastSpectateSecretRedeem() + 1000 > System.currentTimeMillis())) {
                        return;
                    }
                    options.setLastSpectateSecretRedeem(System.currentTimeMillis());

                    this.connectTo(player, secretOwner);
                });
            }
        }
    }

    private void connectTo(ProxiedPlayer player, ICloudPlayer target) {
        ServiceInfoSnapshot connectedService = BridgeProxyHelper.getCachedServiceInfoSnapshot(target.getConnectedService().getServerName());

        if (connectedService == null) {
            BridgePlayerManager.getInstance().updateOnlinePlayer(target);
            return;
        }

        byte[] discordRPCData = LabyModUtils.getDiscordRPCGameInfoUpdateMessageContents(target, connectedService);
        if (discordRPCData != null) {
            BridgePlayerManager.getInstance().getPlayerExecutor(target).sendPluginMessage(LabyModConstants.LMC_CHANNEL_NAME, discordRPCData);
        }

        player.connect(ProxyServer.getInstance().getServerInfo(target.getConnectedService().getServerName()));
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
