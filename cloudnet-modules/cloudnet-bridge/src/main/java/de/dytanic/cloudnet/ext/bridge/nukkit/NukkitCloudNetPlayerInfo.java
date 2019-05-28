package de.dytanic.cloudnet.ext.bridge.nukkit;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

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