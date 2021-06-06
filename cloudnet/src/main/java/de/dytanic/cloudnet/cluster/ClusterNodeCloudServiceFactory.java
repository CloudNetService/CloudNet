package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.provider.service.RemoteCloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Deprecated
@ApiStatus.ScheduledForRemoval
public class ClusterNodeCloudServiceFactory extends RemoteCloudServiceFactory {

  private final IClusterNodeServer server;

  public ClusterNodeCloudServiceFactory(Supplier<INetworkChannel> channelSupplier, IClusterNodeServer server) {
    super(channelSupplier);
    this.server = server;
  }

  @Override
  public @Nullable ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
    return super.createCloudService(this.prepareConfiguration(serviceConfiguration));
  }

  @Override
  public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
    return super.createCloudServiceAsync(this.prepareConfiguration(serviceConfiguration));
  }

  private ServiceConfiguration prepareConfiguration(ServiceConfiguration serviceConfiguration) {
    serviceConfiguration.getServiceId().setNodeUniqueId(this.server.getNodeInfo().getUniqueId());
    return serviceConfiguration;
  }

}
