package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.lang.reflect.Type;
import java.util.UUID;

public class CloudPlayer extends CloudOfflinePlayer implements ICloudPlayer {

    public static final Type TYPE = new TypeToken<CloudPlayer>() {
    }.getType();

    protected NetworkServiceInfo loginService, connectedService;

    protected NetworkConnectionInfo networkConnectionInfo;

    protected NetworkPlayerServerInfo networkPlayerServerInfo;

    public CloudPlayer(ICloudOfflinePlayer cloudOfflinePlayer,
                       NetworkServiceInfo loginService,
                       NetworkServiceInfo connectedService,
                       NetworkConnectionInfo networkConnectionInfo,
                       NetworkPlayerServerInfo networkPlayerServerInfo) {
        super(
                cloudOfflinePlayer.getUniqueId(),
                cloudOfflinePlayer.getName(),
                cloudOfflinePlayer.getXBoxId(),
                cloudOfflinePlayer.getFirstLoginTimeMillis(),
                cloudOfflinePlayer.getLastLoginTimeMillis(),
                cloudOfflinePlayer.getLastNetworkConnectionInfo()
        );

        //
        this.properties = cloudOfflinePlayer.getProperties();
        //

        this.loginService = loginService;
        this.connectedService = connectedService;
        this.networkConnectionInfo = networkConnectionInfo;
        this.networkPlayerServerInfo = networkPlayerServerInfo;
    }

    public CloudPlayer(
            UUID uniqueId,
            String name,
            String xBoxId,
            long firstLoginTimeMillis,
            long lastLoginTimeMillis,
            NetworkConnectionInfo lastNetworkConnectionInfo,
            NetworkServiceInfo loginService,
            NetworkServiceInfo connectedService,
            NetworkConnectionInfo networkConnectionInfo,
            NetworkPlayerServerInfo networkPlayerServerInfo,
            JsonDocument properties
    ) {
        super(uniqueId, name, xBoxId, firstLoginTimeMillis, lastLoginTimeMillis, lastNetworkConnectionInfo);
        this.loginService = loginService;
        this.connectedService = connectedService;
        this.networkConnectionInfo = networkConnectionInfo;
        this.networkPlayerServerInfo = networkPlayerServerInfo;
        this.properties = properties;
    }

    public CloudPlayer(NetworkServiceInfo loginService, NetworkServiceInfo connectedService, NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        this.loginService = loginService;
        this.connectedService = connectedService;
        this.networkConnectionInfo = networkConnectionInfo;
        this.networkPlayerServerInfo = networkPlayerServerInfo;
    }

    public CloudPlayer() {
    }

    public NetworkServiceInfo getLoginService() {
        return this.loginService;
    }

    public NetworkServiceInfo getConnectedService() {
        return this.connectedService;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }

    public NetworkPlayerServerInfo getNetworkPlayerServerInfo() {
        return this.networkPlayerServerInfo;
    }

    public void setLoginService(NetworkServiceInfo loginService) {
        this.loginService = loginService;
    }

    public void setConnectedService(NetworkServiceInfo connectedService) {
        this.connectedService = connectedService;
    }

    public void setNetworkConnectionInfo(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public void setNetworkPlayerServerInfo(NetworkPlayerServerInfo networkPlayerServerInfo) {
        this.networkPlayerServerInfo = networkPlayerServerInfo;
    }

    public String toString() {
        return "CloudPlayer(loginService=" + this.getLoginService() + ", connectedService=" + this.getConnectedService() + ", networkConnectionInfo=" + this.getNetworkConnectionInfo() + ", networkPlayerServerInfo=" + this.getNetworkPlayerServerInfo() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CloudPlayer)) return false;
        final CloudPlayer other = (CloudPlayer) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$loginService = this.getLoginService();
        final Object other$loginService = other.getLoginService();
        if (this$loginService == null ? other$loginService != null : !this$loginService.equals(other$loginService))
            return false;
        final Object this$connectedService = this.getConnectedService();
        final Object other$connectedService = other.getConnectedService();
        if (this$connectedService == null ? other$connectedService != null : !this$connectedService.equals(other$connectedService))
            return false;
        final Object this$networkConnectionInfo = this.getNetworkConnectionInfo();
        final Object other$networkConnectionInfo = other.getNetworkConnectionInfo();
        if (this$networkConnectionInfo == null ? other$networkConnectionInfo != null : !this$networkConnectionInfo.equals(other$networkConnectionInfo))
            return false;
        final Object this$networkPlayerServerInfo = this.getNetworkPlayerServerInfo();
        final Object other$networkPlayerServerInfo = other.getNetworkPlayerServerInfo();
        if (this$networkPlayerServerInfo == null ? other$networkPlayerServerInfo != null : !this$networkPlayerServerInfo.equals(other$networkPlayerServerInfo))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CloudPlayer;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $loginService = this.getLoginService();
        result = result * PRIME + ($loginService == null ? 43 : $loginService.hashCode());
        final Object $connectedService = this.getConnectedService();
        result = result * PRIME + ($connectedService == null ? 43 : $connectedService.hashCode());
        final Object $networkConnectionInfo = this.getNetworkConnectionInfo();
        result = result * PRIME + ($networkConnectionInfo == null ? 43 : $networkConnectionInfo.hashCode());
        final Object $networkPlayerServerInfo = this.getNetworkPlayerServerInfo();
        result = result * PRIME + ($networkPlayerServerInfo == null ? 43 : $networkPlayerServerInfo.hashCode());
        return result;
    }
}