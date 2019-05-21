package de.dytanic.cloudnet.driver.network.cluster;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NetworkClusterNode extends BasicJsonDocPropertyable {

    private final String uniqueId;

    private final HostAndPort[] listeners;

}