package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.DriverCommandSender;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.provider.service.EmptySpecificCloudServiceProvider;
import de.dytanic.cloudnet.provider.service.LocalNodeSpecificCloudServiceProvider;
import de.dytanic.cloudnet.service.ICloudService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

final class LocalNodeServer extends DefaultNodeServer {

    private final CloudNet cloudNet;

    LocalNodeServer(NodeServerProvider provider, CloudNet cloudNet) {
        super(provider, cloudNet.getConfig().getIdentity());
        this.setNodeInfoSnapshot(new NetworkClusterNodeInfoSnapshot(
                System.currentTimeMillis(),
                System.nanoTime(),
                cloudNet.getConfig().getIdentity(),
                CloudNet.class.getPackage().getImplementationVersion(),
                0,
                0,
                0,
                cloudNet.getConfig().getMaxMemory(),
                ProcessSnapshot.self(),
                cloudNet.getModuleProvider().getModules().stream().map(IModuleWrapper::getModuleConfiguration).collect(Collectors.toList()),
                CPUUsageResolver.getSystemCPUUsage()
        ));
        this.cloudNet = cloudNet;
    }

    @Override
    public @NotNull NetworkClusterNode getNodeInfo() {
        return this.cloudNet.getConfig().getIdentity();
    }

    @Override
    public void setNodeInfo(@NotNull NetworkClusterNode nodeInfo) {
        this.cloudNet.getConfig().setIdentity(nodeInfo);
    }

    @Override
    public String[] sendCommandLine(@NotNull String commandLine) {
        Collection<String> result = new ArrayList<>();
        this.cloudNet.getCommandMap().dispatchCommand(new DriverCommandSender(result), commandLine);
        return result.toArray(new String[0]);
    }

    @Override
    public @NotNull CloudServiceFactory getCloudServiceFactory() {
        return this.cloudNet.getCloudServiceFactory();
    }

    @Override
    public SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot info) {
        ICloudService service = this.cloudNet.getCloudServiceManager().getCloudService(info.getServiceId().getUniqueId());
        return service == null
                ? EmptySpecificCloudServiceProvider.INSTANCE
                : new LocalNodeSpecificCloudServiceProvider(this.cloudNet, service);
    }

    @Override
    public void close() {
        this.cloudNet.stop();
    }
}
