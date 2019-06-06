package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.lang.reflect.Type;
import java.util.UUID;

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

    public String toString() {
        return "CloudOfflinePlayer(uniqueId=" + this.getUniqueId() + ", name=" + this.getName() + ", xBoxId=" + this.getXBoxId() + ", firstLoginTimeMillis=" + this.getFirstLoginTimeMillis() + ", lastLoginTimeMillis=" + this.getLastLoginTimeMillis() + ", lastNetworkConnectionInfo=" + this.getLastNetworkConnectionInfo() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CloudOfflinePlayer)) return false;
        final CloudOfflinePlayer other = (CloudOfflinePlayer) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$xBoxId = this.getXBoxId();
        final Object other$xBoxId = other.getXBoxId();
        if (this$xBoxId == null ? other$xBoxId != null : !this$xBoxId.equals(other$xBoxId)) return false;
        if (this.getFirstLoginTimeMillis() != other.getFirstLoginTimeMillis()) return false;
        if (this.getLastLoginTimeMillis() != other.getLastLoginTimeMillis()) return false;
        final Object this$lastNetworkConnectionInfo = this.getLastNetworkConnectionInfo();
        final Object other$lastNetworkConnectionInfo = other.getLastNetworkConnectionInfo();
        if (this$lastNetworkConnectionInfo == null ? other$lastNetworkConnectionInfo != null : !this$lastNetworkConnectionInfo.equals(other$lastNetworkConnectionInfo))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CloudOfflinePlayer;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $xBoxId = this.getXBoxId();
        result = result * PRIME + ($xBoxId == null ? 43 : $xBoxId.hashCode());
        final long $firstLoginTimeMillis = this.getFirstLoginTimeMillis();
        result = result * PRIME + (int) ($firstLoginTimeMillis >>> 32 ^ $firstLoginTimeMillis);
        final long $lastLoginTimeMillis = this.getLastLoginTimeMillis();
        result = result * PRIME + (int) ($lastLoginTimeMillis >>> 32 ^ $lastLoginTimeMillis);
        final Object $lastNetworkConnectionInfo = this.getLastNetworkConnectionInfo();
        result = result * PRIME + ($lastNetworkConnectionInfo == null ? 43 : $lastNetworkConnectionInfo.hashCode());
        return result;
    }
}