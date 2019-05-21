package de.dytanic.cloudnet.ext.bridge.gomint;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Locale;
import java.util.UUID;

@Data
@AllArgsConstructor
final class GoMintCloudNetPlayerInfo {

    private UUID uniqueId;

    private boolean online;

    private String name, deviceName, xBoxId, gamemode;

    protected double health, maxHealth, saturation;

    protected int level, ping;

    protected Locale locale;

    protected WorldPosition location;

    protected HostAndPort address;

}