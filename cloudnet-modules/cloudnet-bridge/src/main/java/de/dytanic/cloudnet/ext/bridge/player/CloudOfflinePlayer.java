package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;
import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = false)
public class CloudOfflinePlayer extends BasicJsonDocPropertyable implements ICloudOfflinePlayer {

    public static final Type TYPE = new TypeToken<CloudOfflinePlayer>() {
    }.getType();

    protected UUID uniqueId;

    protected String name, xBoxId;

    protected long firstLoginTimeMillis, lastLoginTimeMillis;

    protected NetworkConnectionInfo lastNetworkConnectionInfo;

    public CloudOfflinePlayer(UUID uniqueId, String name, String xBoxId, long firstLoginTimeMillis, long lastLoginTimeMillis, NetworkConnectionInfo lastNetworkConnectionInfo) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.xBoxId = xBoxId;
        this.firstLoginTimeMillis = firstLoginTimeMillis;
        this.lastLoginTimeMillis = lastLoginTimeMillis;
        this.lastNetworkConnectionInfo = lastNetworkConnectionInfo;
    }

    public CloudOfflinePlayer() {
    }

    /*= --------------------------------------------------------------- =*/

    public static CloudOfflinePlayer of(ICloudPlayer cloudPlayer) {
        Validate.checkNotNull(cloudPlayer);

        CloudOfflinePlayer cloudOfflinePlayer = new CloudOfflinePlayer(
                cloudPlayer.getUniqueId(),
                cloudPlayer.getName(),
                cloudPlayer.getXBoxId(),
                cloudPlayer.getFirstLoginTimeMillis(),
                cloudPlayer.getLastLoginTimeMillis(),
                cloudPlayer.getLastNetworkConnectionInfo()
        );

        cloudOfflinePlayer.setProperties(cloudPlayer.getProperties());

        return cloudOfflinePlayer;
    }

    @Override
    public void setProperties(JsonDocument properties) {
        this.properties = properties;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public String getXBoxId() {
        return this.xBoxId;
    }

    public long getFirstLoginTimeMillis() {
        return this.firstLoginTimeMillis;
    }

    public long getLastLoginTimeMillis() {
        return this.lastLoginTimeMillis;
    }

    public NetworkConnectionInfo getLastNetworkConnectionInfo() {
        return this.lastNetworkConnectionInfo;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setXBoxId(String xBoxId) {
        this.xBoxId = xBoxId;
    }

    public void setFirstLoginTimeMillis(long firstLoginTimeMillis) {
        this.firstLoginTimeMillis = firstLoginTimeMillis;
    }

    public void setLastLoginTimeMillis(long lastLoginTimeMillis) {
        this.lastLoginTimeMillis = lastLoginTimeMillis;
    }

    public void setLastNetworkConnectionInfo(NetworkConnectionInfo lastNetworkConnectionInfo) {
        this.lastNetworkConnectionInfo = lastNetworkConnectionInfo;
    }

}