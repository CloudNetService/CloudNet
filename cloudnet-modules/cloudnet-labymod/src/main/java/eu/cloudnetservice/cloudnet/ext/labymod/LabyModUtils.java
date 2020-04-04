package eu.cloudnetservice.cloudnet.ext.labymod;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.labymod.config.DiscordJoinMatchConfig;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.cloudnet.ext.labymod.config.ServiceDisplay;
import eu.cloudnetservice.cloudnet.ext.labymod.player.LabyModPlayerOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static eu.cloudnetservice.cloudnet.ext.labymod.LabyModChannelUtils.getLMCMessageContents;

public class LabyModUtils {

    private static LabyModConfiguration cachedConfiguration;

    private LabyModUtils() {
        throw new UnsupportedOperationException();
    }

    public static LabyModConfiguration getConfiguration() {
        if (cachedConfiguration == null) {
            ITask<LabyModConfiguration> task = CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacket(CloudNetDriver.getInstance().getNetworkClient().getChannels().iterator().next(),
                    LabyModConstants.CLOUDNET_CHANNEL_NAME,
                    LabyModConstants.GET_CONFIGURATION,
                    new JsonDocument(),
                    documentPair -> documentPair.get("labyModConfig", LabyModConfiguration.class));
            try {
                cachedConfiguration = task.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }

        return cachedConfiguration;
    }

    public static void setLabyModOptions(ICloudPlayer cloudPlayer, LabyModPlayerOptions options) {
        cloudPlayer.getOnlineProperties().append("labyModOptions", options);
    }

    public static LabyModPlayerOptions getLabyModOptions(ICloudPlayer cloudPlayer) {
        return cloudPlayer.getOnlineProperties().get("labyModOptions", LabyModPlayerOptions.class);
    }

    @NotNull
    public static ITask<ICloudPlayer> getPlayerByJoinSecret(UUID joinSecret) {
        return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacket(CloudNetDriver.getInstance().getNetworkClient().getChannels().iterator().next(),
                LabyModConstants.CLOUDNET_CHANNEL_NAME,
                LabyModConstants.GET_PLAYER_JOIN_SECRET,
                new JsonDocument().append("joinSecret", joinSecret),
                document -> document.get("player", CloudPlayer.TYPE));
    }

    @NotNull
    public static ITask<ICloudPlayer> getPlayerBySpectateSecret(UUID spectateSecret) {
        return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacket(CloudNetDriver.getInstance().getNetworkClient().getChannels().iterator().next(),
                LabyModConstants.CLOUDNET_CHANNEL_NAME,
                LabyModConstants.GET_PLAYER_SPECTATE_SECRET,
                new JsonDocument().append("spectateSecret", spectateSecret),
                document -> document.get("player", CloudPlayer.TYPE));
    }

    private static String getDisplay(ServiceInfoSnapshot serviceInfoSnapshot, ServiceDisplay serviceDisplay) {
        if (serviceDisplay == null || !serviceDisplay.isEnabled()) {
            return null;
        }

        return serviceDisplay.getDisplay(serviceInfoSnapshot);
    }

    public static byte[] getShowGameModeMessageContents(ServiceInfoSnapshot serviceInfoSnapshot) {
        String display = getDisplay(serviceInfoSnapshot, getConfiguration().getGameModeSwitchMessages());
        if (display == null) {
            return null;
        }

        JsonDocument document = JsonDocument.newDocument();
        document.append("show_gamemode", true).append("gamemode_name", display);

        return getLMCMessageContents("server_gamemode", document);
    }

    public static boolean canSpectate(ServiceInfoSnapshot serviceInfoSnapshot) {
        return getConfiguration().isDiscordSpectateEnabled() &&
                !isExcluded(getConfiguration().getExcludedSpectateGroups(), serviceInfoSnapshot.getConfiguration().getGroups()) &&
                serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false);
    }

    public static byte[] getDiscordRPCGameInfoUpdateMessageContents(ICloudPlayer cloudPlayer, ServiceInfoSnapshot serviceInfoSnapshot) {
        String display = getDisplay(serviceInfoSnapshot, getConfiguration().getDiscordRPC());
        if (display == null) {
            return null;
        }

        DiscordJoinMatchConfig joinMatchConfig = getConfiguration().getDiscordJoinMatch();
        boolean joinSecret = false;
        boolean spectateSecret = false;

        boolean modified = false;

        LabyModPlayerOptions options = getLabyModOptions(cloudPlayer);
        if (options == null) {
            return null;
        }

        if (joinMatchConfig != null && joinMatchConfig.isEnabled() && !joinMatchConfig.isExcluded(serviceInfoSnapshot)) {
            options.createNewJoinSecret();

            joinSecret = true;
            modified = true;
        } else if (options.getJoinSecret() != null) {
            options.removeJoinSecret();

            modified = true;
        }

        if (canSpectate(serviceInfoSnapshot)) {
            options.createNewSpectateSecret();

            spectateSecret = true;
            modified = true;
        } else if (options.getSpectateSecret() != null) {
            options.removeSpectateSecret();
            modified = true;
        }

        if (modified) {
            setLabyModOptions(cloudPlayer, options);
            CloudNetDriver.getInstance().getServicesRegistry().getService(IPlayerManager.class).updateOnlinePlayer(cloudPlayer);
        }

        JsonDocument document = JsonDocument.newDocument();
        document.append("hasGame", true);

        document.append("game_mode", display)
                .append("game_startTime", 0)
                .append("game_endTime", 0);

        String domain = getConfiguration().getLoginDomain();

        document.append("hasJoinSecret", joinSecret);
        if (joinSecret) {
            document.append("joinSecret", options.getJoinSecret() + ":" + domain);
        }
        document.append("hasMatchSecret", cloudPlayer.getConnectedService() != null);
        if (cloudPlayer.getConnectedService() != null) {
            document.append("matchSecret", cloudPlayer.getConnectedService().getUniqueId() + ":" + domain);
        }
        document.append("hasSpectateSecret", spectateSecret);
        if (spectateSecret) {
            document.append("spectateSecret", options.getSpectateSecret() + ":" + domain);
        }

        return getLMCMessageContents("discord_rpc", document);
    }

    public static boolean isExcluded(Collection<String> excludedGroups, String[] serviceGroups) {
        for (String excludedGroup : excludedGroups) {
            if (Arrays.asList(serviceGroups).contains(excludedGroup)) {
                return true;
            }
        }
        return false;
    }

}
