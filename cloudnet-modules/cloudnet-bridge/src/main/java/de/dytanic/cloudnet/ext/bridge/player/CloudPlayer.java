package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
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
                       NetworkPlayerServerInfo networkPlayerServerInfo)
    {
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
    )
    {
        super(uniqueId, name, xBoxId, firstLoginTimeMillis, lastLoginTimeMillis, lastNetworkConnectionInfo);
        this.loginService = loginService;
        this.connectedService = connectedService;
        this.networkConnectionInfo = networkConnectionInfo;
        this.networkPlayerServerInfo = networkPlayerServerInfo;
        this.properties = properties;
    }
}