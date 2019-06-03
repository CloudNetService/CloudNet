package de.dytanic.cloudnet.ext.bridge.proxprox;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Locale;
import java.util.UUID;

@Data
@AllArgsConstructor
final class ProxProxCloudNetPlayerInfo {

    private UUID uniqueId;

    private Locale locale;

    private String name, xBoxId;

    private HostAndPort address, connectedServer;

    private long ping;

}