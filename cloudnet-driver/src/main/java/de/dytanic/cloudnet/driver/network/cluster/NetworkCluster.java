package de.dytanic.cloudnet.driver.network.cluster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkCluster {

    private UUID clusterId;

    private Collection<NetworkClusterNode> nodes;

}