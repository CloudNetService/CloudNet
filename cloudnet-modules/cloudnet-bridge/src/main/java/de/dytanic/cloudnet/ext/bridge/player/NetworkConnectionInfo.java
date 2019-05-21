package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkConnectionInfo {

    public static final Type TYPE = new TypeToken<NetworkConnectionInfo>() {
    }.getType();

    protected UUID uniqueId;

    protected String name;

    protected int version;

    protected HostAndPort address, listener;

    protected boolean onlineMode, legacy;

    protected NetworkServiceInfo networkService;

}