package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;

public interface ICloudService {

    @ApiStatus.Internal
    void init();

    @Nullable
    String getJavaCommand();

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
    @Deprecated
    File getDirectory();

    @NotNull
    Path getDirectoryPath();

    INetworkChannel getNetworkChannel();

    @ApiStatus.Internal
    void setNetworkChannel(INetworkChannel channel);

    @NotNull
    ServiceInfoSnapshot getServiceInfoSnapshot();

    @NotNull
    ServiceInfoSnapshot getLastServiceInfoSnapshot();

    ITask<ServiceInfoSnapshot> forceUpdateServiceInfoSnapshotAsync();

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

    default void delete() {
        this.delete(true);
    }

    void delete(boolean sendUpdate);

    boolean isAlive();

    void includeInclusions();

    void includeTemplates();

    void deployResources(boolean removeDeployments);

    default void deployResources() {
        this.deployResources(true);
    }

    void offerTemplate(@NotNull ServiceTemplate template);

    void offerInclusion(@NotNull ServiceRemoteInclusion inclusion);

    void addDeployment(@NotNull ServiceDeployment deployment);

    void updateServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);

}