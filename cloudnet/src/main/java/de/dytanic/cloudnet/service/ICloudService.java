package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.service.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Queue;

public interface ICloudService {

    @NotNull
    String getRuntime();

    List<ServiceRemoteInclusion> getIncludes();

    List<ServiceTemplate> getTemplates();

    List<ServiceDeployment> getDeployments();

    Queue<ServiceRemoteInclusion> getWaitingIncludes();

    Queue<ServiceTemplate> getWaitingTemplates();

    List<String> getGroups();

    @NotNull
    ServiceLifeCycle getLifeCycle();

    @NotNull
    ICloudServiceManager getCloudServiceManager();

    @NotNull
    ServiceConfiguration getServiceConfiguration();

    @NotNull
    ServiceId getServiceId();

    String getConnectionKey();

    @NotNull
    File getDirectory();

    INetworkChannel getNetworkChannel();

    void setNetworkChannel(INetworkChannel channel);

    @NotNull
    ServiceInfoSnapshot getServiceInfoSnapshot();

    void setServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);

    @NotNull
    ServiceInfoSnapshot getLastServiceInfoSnapshot();

    @Nullable
    Process getProcess();

    void runCommand(@NotNull String commandLine);

    int getConfiguredMaxHeapMemory();

    @NotNull
    IServiceConsoleLogCache getServiceConsoleLogCache();


    void start() throws Exception;

    void restart() throws Exception;

    int stop();

    int kill();

    void delete();

    boolean isAlive();

    void includeInclusions();

    void includeTemplates();

    void deployResources(boolean removeDeployments);

    default void deployResources() {
        deployResources(true);
    }

    void offerTemplate(@NotNull ServiceTemplate template);

    void offerInclusion(@NotNull ServiceRemoteInclusion inclusion);

    void addDeployment(@NotNull ServiceDeployment deployment);

    default void updateServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        this.setServiceInfoSnapshot(serviceInfoSnapshot);
        this.getCloudServiceManager().getGlobalServiceInfoSnapshots().put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);

        CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceInfoUpdateEvent(serviceInfoSnapshot));

        CloudNet.getInstance().getNetworkClient()
                .sendPacket(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.UPDATE));
        CloudNet.getInstance().getNetworkServer()
                .sendPacket(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.UPDATE));
    }

}