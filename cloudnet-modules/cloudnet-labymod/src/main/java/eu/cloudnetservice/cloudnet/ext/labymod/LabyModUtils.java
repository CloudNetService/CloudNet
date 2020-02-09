package eu.cloudnetservice.cloudnet.ext.labymod;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import eu.cloudnetservice.cloudnet.ext.labymod.player.DiscordJoinMatchConfig;
import eu.cloudnetservice.cloudnet.ext.labymod.player.LabyModPlayerOptions;
import eu.cloudnetservice.cloudnet.ext.labymod.player.ServiceDisplay;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LabyModUtils {

    private static LabyModConfiguration cachedConfiguration;

    private LabyModUtils() {
        throw new UnsupportedOperationException();
    }

    public static byte[] getLMCMessageContents(String messageKey, JsonDocument messageContents) {
        ByteBuf byteBuf = Unpooled.buffer();
        writeString(byteBuf, messageKey);
        writeString(byteBuf, messageContents.toJson());

        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        return bytes;
    }

    public static Pair<String, JsonDocument> readLMCMessageContents(byte[] data) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(data);

        String messageKey = readString(byteBuf);
        String messageContents = readString(byteBuf);
        JsonDocument document = JsonDocument.newDocument(messageContents);

        return new Pair<>(messageKey, document);
    }

    public static int readVarInt(ByteBuf byteBuf) {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = byteBuf.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    public static ByteBuf writeVarInt(ByteBuf byteBuf, int value) {
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            byteBuf.writeByte(temp);
        } while (value != 0);

        return byteBuf;
    }

    public static ByteBuf writeString(ByteBuf byteBuf, String string) {
        byte[] values = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(byteBuf, values.length);
        byteBuf.writeBytes(values);
        return byteBuf;
    }

    public static String readString(ByteBuf byteBuf) {
        int integer = readVarInt(byteBuf);
        byte[] buffer = new byte[integer];
        byteBuf.readBytes(buffer, 0, integer);

        return new String(buffer, StandardCharsets.UTF_8);
    }

    public static LabyModConfiguration getConfiguration() {
        if (cachedConfiguration == null) {
            ITask<LabyModConfiguration> task = CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacket(CloudNetDriver.getInstance().getNetworkClient().getChannels().iterator().next(),
                    LabyModConstants.GET_CONFIGURATION_CHANNEL_NAME,
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
        cloudPlayer.getProperties().append("labyModOptions", options);
    }

    public static LabyModPlayerOptions getLabyModOptions(ICloudPlayer cloudPlayer) {
        return cloudPlayer.getProperties().get("labyModOptions", LabyModPlayerOptions.class);
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

    public static byte[] getDiscordRPCGameInfoUpdateMessageContents(ICloudPlayer cloudPlayer, ServiceInfoSnapshot serviceInfoSnapshot) {
        String display = getDisplay(serviceInfoSnapshot, getConfiguration().getDiscordRPC());
        if (display == null) {
            return null;
        }

        DiscordJoinMatchConfig joinMatchConfig = getConfiguration().getDiscordJoinMatch();
        boolean joinSecret = false;

        LabyModPlayerOptions options = getLabyModOptions(cloudPlayer);
        if (options == null) {
            return null;
        }

        if (joinMatchConfig != null && joinMatchConfig.isEnabled() && !joinMatchConfig.isExcluded(serviceInfoSnapshot)) {
            options.createNewJoinSecret();
            setLabyModOptions(cloudPlayer, options);
            BridgePlayerManager.getInstance().updateOnlinePlayer(cloudPlayer);

            joinSecret = true;
        } else if (options.getJoinSecret() != null) {
            options.removeJoinSecret();
            setLabyModOptions(cloudPlayer, options);
            BridgePlayerManager.getInstance().updateOnlinePlayer(cloudPlayer);
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
        /*document.append("hasSpectateSecret", true)
                .append("spectateSecret", cloudPlayer.getLabyModOptions().getJoinSecret() + ":" + domain);*/

        return getLMCMessageContents("discord_rpc", document);
    }

}
