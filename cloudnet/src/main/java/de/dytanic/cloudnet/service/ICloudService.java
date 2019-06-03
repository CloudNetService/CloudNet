package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
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

    /*= ------------------------------------------------------------------- =*/

    void start() throws Exception;

    void restart() throws Exception;

    int stop();

    int kill();

    void delete();

    boolean isAlive();

    void includeInclusions();

    void includeTemplates();

    void deployResources();
}