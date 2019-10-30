package de.dytanic.cloudnet.driver.service.provider;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.*;

import java.util.Collection;
import java.util.Queue;
import java.util.UUID;

public interface SpecificCloudServiceProvider {

    ServiceInfoSnapshot getServiceInfoSnapshot();

    ITask<ServiceInfoSnapshot> getServiceInfoSnapshotAsync();

    void addServiceTemplate(ServiceTemplate serviceTemplate);

    ITask<Void> addServiceTemplateAsync(ServiceTemplate serviceTemplate);

    void addServiceRemoteInclusion(ServiceRemoteInclusion serviceRemoteInclusion);

    ITask<Void> addServiceRemoteInclusionAsync(ServiceRemoteInclusion serviceRemoteInclusion);

    void addServiceDeployment(ServiceDeployment serviceDeployment);

    ITask<Void> addServiceDeploymentAsync(ServiceDeployment serviceDeployment);

    Queue<String> getCachedLogMessages();

    ITask<Queue<String>> getCachedLogMessagesAsync();

    default void stop() {
        this.setCloudServiceLifeCycle(ServiceLifeCycle.STOPPED);
    }

    default void start() {
        this.setCloudServiceLifeCycle(ServiceLifeCycle.RUNNING);
    }

    default void delete() {
        this.setCloudServiceLifeCycle(ServiceLifeCycle.DELETED);
    }

    void setCloudServiceLifeCycle(ServiceLifeCycle lifeCycle);

    void restart();

    ITask<Void> restartAsync();

    void kill();

    ITask<Void> killAsync();

    void runCommand(String command);

    ITask<Void> runCommandAsync(String command);

    void includeWaitingServiceTemplates();

    void includeWaitingServiceInclusions();

    void deployResources(boolean removeDeployments);

    default void deployResources() {
        this.deployResources(true);
    }

    ITask<Void> includeWaitingServiceTemplatesAsync();

    ITask<Void> includeWaitingServiceInclusionsAsync();

    ITask<Void> deployResourcesAsync(boolean removeDeployments);

    default ITask<Void> deployResourcesAsync() {
        return this.deployResourcesAsync(true);
    }

}
