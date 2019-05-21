package de.dytanic.cloudnet.ext.bridge.nukkit;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
final class NukkitCloudNetPlayerInfo {

    private UUID uniqueId;

    private String name;

    protected double health, maxHealth, saturation;

    protected int level, ping;

    protected WorldPosition location;

    protected HostAndPort address;

}