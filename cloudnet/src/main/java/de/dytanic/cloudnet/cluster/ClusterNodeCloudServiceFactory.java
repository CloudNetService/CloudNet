package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.provider.service.RemoteCloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ClusterNodeCloudServiceFactory extends RemoteCloudServiceFactory {

    private final IClusterNodeServer server;

    public ClusterNodeCloudServiceFactory(Supplier<INetworkChannel> channelSupplier, IClusterNodeServer server) {
        super(channelSupplier);
        this.server = server;
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask) {
        return super.createCloudServiceAsync(this.prepareServiceTask(serviceTask));
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask, int taskId) {
        return super.createCloudServiceAsync(this.prepareServiceTask(serviceTask), taskId);
    }

    private ServiceTask prepareServiceTask(ServiceTask serviceTask) {
        ServiceTask clone = serviceTask.makeClone();
        clone.getAssociatedNodes().clear();
        clone.getAssociatedNodes().add(this.server.getNodeInfo().getUniqueId());
        return clone;
    }

}
