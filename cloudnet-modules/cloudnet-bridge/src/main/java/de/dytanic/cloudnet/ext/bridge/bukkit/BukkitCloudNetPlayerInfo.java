package de.dytanic.cloudnet.ext.bridge.bukkit;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
final class BukkitCloudNetPlayerInfo {

    protected UUID uniqueId;

    protected String name;

    protected double health, maxHealth, saturation;

    protected int level;

    protected WorldPosition location;

    protected HostAndPort address;

}