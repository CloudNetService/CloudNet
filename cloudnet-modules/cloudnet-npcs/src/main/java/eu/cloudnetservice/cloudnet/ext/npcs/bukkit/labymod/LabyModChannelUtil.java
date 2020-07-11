package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.labymod;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public class LabyModChannelUtil {

    private static final Gson GSON = new Gson();

    private LabyModChannelUtil() {
        throw new UnsupportedOperationException();
    }

    public static byte[] createChannelMessageData(String messageKey, JsonElement jsonElement) {
        return ProtocolBuffer.create()
                .writeString(messageKey)
                .writeString(GSON.toJson(jsonElement))
                .toArray();
    }

}
