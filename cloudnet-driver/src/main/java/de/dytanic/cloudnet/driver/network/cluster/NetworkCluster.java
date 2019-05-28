package de.dytanic.cloudnet.driver.network.cluster;

import java.util.Collection;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkCluster {

  private UUID clusterId;

  private Collection<NetworkClusterNode> nodes;

}