package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.service.*;

import java.io.File;
import java.util.List;
import java.util.Queue;

public interface ICloudService {

    String getRuntime();

    List<ServiceRemoteInclusion> getIncludes();

    List<ServiceTemplate> getTemplates();

    List<ServiceDeployment> getDeployments();

    Queue<ServiceRemoteInclusion> getWaitingIncludes();

    Queue<ServiceTemplate> getWaitingTemplates();

    List<String> getGroups();

    ServiceLifeCycle getLifeCycle();

    ICloudServiceManager getCloudServiceManager();

    ServiceConfiguration getServiceConfiguration();

    ServiceId getServiceId();

    String getConnectionKey();

    File getDirectory();

    INetworkChannel getNetworkChannel();

    void setNetworkChannel(INetworkChannel channel);

    ServiceInfoSnapshot getServiceInfoSnapshot();

    void setServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot);

    ServiceInfoSnapshot getLastServiceInfoSnapshot();

    Process getProcess();

    void runCommand(String commandLine);

    int getConfiguredMaxHeapMemory();

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

    void offerTemplate(ServiceTemplate template);

    void offerInclusion(ServiceRemoteInclusion inclusion);

    void addDeployment(ServiceDeployment deployment);

    default void updateServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.setServiceInfoSnapshot(serviceInfoSnapshot);
        this.getCloudServiceManager().getGlobalServiceInfoSnapshots().put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);

        CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceInfoUpdateEvent(serviceInfoSnapshot));

        CloudNet.getInstance().getNetworkClient()
                .sendPacket(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.UPDATE));
        CloudNet.getInstance().getNetworkServer()
                .sendPacket(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.UPDATE));
    }

}