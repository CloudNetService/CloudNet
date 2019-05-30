package de.dytanic.cloudnet.ext.bridge.sponge;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
final class SpongeCloudNetPlayerInfo {

  private UUID uniqueId;

  private String name;

  private int ping;

  private double health, maxHealth, saturation;

  private int level;

  private WorldPosition location;

  private HostAndPort address;

}