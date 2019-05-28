package de.dytanic.cloudnet.ext.bridge.bungee;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
final class BungeeCloudNetPlayerInfo {

  private UUID uniqueId;

  private String name, server;

  private int ping;

  private HostAndPort address;

}