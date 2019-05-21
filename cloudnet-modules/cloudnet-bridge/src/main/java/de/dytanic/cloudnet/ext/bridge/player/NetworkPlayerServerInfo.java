package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class NetworkPlayerServerInfo {

    public static final Type TYPE = new TypeToken<NetworkPlayerServerInfo>() {
    }.getType();

    protected UUID uniqueId;

    protected String name, xBoxId;

    protected double health, maxHealth, saturation;

    protected int level;

    protected WorldPosition location;

    protected HostAndPort address;

    protected NetworkServiceInfo networkService;

}