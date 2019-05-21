package de.dytanic.cloudnet.ext.bridge.velocity;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
final class VelocityCloudNetPlayerInfo {

    private UUID uniqueId;

    private String name, server;

    private int ping;

    private HostAndPort address;

}