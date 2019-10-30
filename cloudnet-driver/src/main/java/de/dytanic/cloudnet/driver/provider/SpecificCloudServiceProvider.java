package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.*;

import java.util.Queue;

public interface SpecificCloudServiceProvider {

    /**
     * Gets the info of the service this provider is for
     *
     * @return the info or {@code null}, if the service doesn't exist
     * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given on creating this provider
     */
    ServiceInfoSnapshot getServiceInfoSnapshot();

    void addServiceTemplate(ServiceTemplate serviceTemplate);

    void addServiceRemoteInclusion(ServiceRemoteInclusion serviceRemoteInclusion);

    void addServiceDeployment(ServiceDeployment serviceDeployment);

    Queue<String> getCachedLogMessages();

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

    void kill();

    void runCommand(String command);

    void includeWaitingServiceTemplates();

    void includeWaitingServiceInclusions();

    void deployResources(boolean removeDeployments);

    default void deployResources() {
        this.deployResources(true);
    }

    /**
     * Gets the info of the service this provider is for
     *
     * @return the info or {@code null}, if the service doesn't exist
     * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given on creating this provider
     */
    ITask<ServiceInfoSnapshot> getServiceInfoSnapshotAsync();

    ITask<Void> addServiceTemplateAsync(ServiceTemplate serviceTemplate);

    ITask<Void> addServiceRemoteInclusionAsync(ServiceRemoteInclusion serviceRemoteInclusion);

    ITask<Void> addServiceDeploymentAsync(ServiceDeployment serviceDeployment);

    ITask<Queue<String>> getCachedLogMessagesAsync();

    default ITask<Void> stopAsync() {
        return this.setCloudServiceLifeCycleAsync(ServiceLifeCycle.STOPPED);
    }

    default ITask<Void> startAsync() {
        return this.setCloudServiceLifeCycleAsync(ServiceLifeCycle.RUNNING);
    }

    default ITask<Void> deleteAsync() {
        return this.setCloudServiceLifeCycleAsync(ServiceLifeCycle.DELETED);
    }

    ITask<Void> setCloudServiceLifeCycleAsync(ServiceLifeCycle lifeCycle);

    ITask<Void> restartAsync();

    ITask<Void> killAsync();

    ITask<Void> runCommandAsync(String command);

    ITask<Void> includeWaitingServiceTemplatesAsync();

    ITask<Void> includeWaitingServiceInclusionsAsync();

    ITask<Void> deployResourcesAsync(boolean removeDeployments);

    default ITask<Void> deployResourcesAsync() {
        return this.deployResourcesAsync(true);
    }

}
