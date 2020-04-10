package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.ext.bridge.player.executor.DefaultPlayerExecutor;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;
import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = false)
public class CloudPlayer extends CloudOfflinePlayer implements ICloudPlayer {

    public static final Type TYPE = new TypeToken<CloudPlayer>() {
    }.getType();

    protected NetworkServiceInfo loginService, connectedService;

    protected NetworkConnectionInfo networkConnectionInfo;

    protected NetworkPlayerServerInfo networkPlayerServerInfo;

    protected JsonDocument onlineProperties;

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
        this.onlineProperties = new JsonDocument();
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

    public void setLoginService(NetworkServiceInfo loginService) {
        this.loginService = loginService;
    }

    public NetworkServiceInfo getConnectedService() {
        return this.connectedService;
    }

    public void setConnectedService(NetworkServiceInfo connectedService) {
        this.connectedService = connectedService;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }

    public void setNetworkConnectionInfo(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public NetworkPlayerServerInfo getNetworkPlayerServerInfo() {
        return this.networkPlayerServerInfo;
    }

    public void setNetworkPlayerServerInfo(NetworkPlayerServerInfo networkPlayerServerInfo) {
        this.networkPlayerServerInfo = networkPlayerServerInfo;
    }

    @Override
    public JsonDocument getOnlineProperties() {
        return this.onlineProperties;
    }

    @Override
    public PlayerExecutor getPlayerExecutor() {
        return new DefaultPlayerExecutor(this.getUniqueId());
    }
}