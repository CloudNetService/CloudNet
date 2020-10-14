package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.document.gson.IJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents the data of an offlinePlayer instance that is saved on the database on CloudNet
 * The player does not need to be online for this item to be provisioned
 */
public interface ICloudOfflinePlayer extends INameable, IJsonDocPropertyable, SerializableObject {

    /**
     * Return the unique identifier of a player from the Minecraft Java or Bedrock Edition
     */
    @NotNull
    UUID getUniqueId();

    /**
     * Sets the players name
     *
     * @param name the name, that should be changed from this object
     */
    void setName(@NotNull String name);

    /**
     * Returns the XBoxId from the offlinePlayer if the player has to be connect from the Minecraft Bedrock Edition
     */
    String getXBoxId();

    long getFirstLoginTimeMillis();

    void setFirstLoginTimeMillis(long firstLoginTimeMillis);

    long getLastLoginTimeMillis();

    void setLastLoginTimeMillis(long lastLoginTimeMillis);

    NetworkConnectionInfo getLastNetworkConnectionInfo();

    void setLastNetworkConnectionInfo(@NotNull NetworkConnectionInfo lastNetworkConnectionInfo);

    JsonDocument getProperties();

    void setProperties(@NotNull JsonDocument properties);
}