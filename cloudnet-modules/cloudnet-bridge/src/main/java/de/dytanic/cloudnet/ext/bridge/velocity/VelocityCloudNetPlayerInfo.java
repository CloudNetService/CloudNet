package de.dytanic.cloudnet.ext.bridge.velocity;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
final class VelocityCloudNetPlayerInfo {

  private UUID uniqueId;

  private String name, server;

  private int ping;

  private HostAndPort address;

}