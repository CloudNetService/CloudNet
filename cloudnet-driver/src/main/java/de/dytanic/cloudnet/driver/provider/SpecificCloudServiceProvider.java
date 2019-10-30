package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.*;

import java.util.Queue;

public interface SpecificCloudServiceProvider {

    /**
     * Gets the info of the service this provider is for.
     *
     * @return the info or {@code null}, if the service doesn't exist
     * @throws IllegalArgumentException if no uniqueId/name/serviceInfo was given on creating this provider
     */
    ServiceInfoSnapshot getServiceInfoSnapshot();

    /**
     * Adds a service template to this service. This template won't be copied directly after adding it but when the service is prepared.
     *
     * @param serviceTemplate the template to be added to the list of templates of this service
     */
    void addServiceTemplate(ServiceTemplate serviceTemplate);

    /**
     * Adds a remote inclusion to this service. This remote inclusion won't be included directly after adding it but when the service is prepared.
     *
     * @param serviceRemoteInclusion the inclusion to be added to the list of inclusions of this service
     */
    void addServiceRemoteInclusion(ServiceRemoteInclusion serviceRemoteInclusion);

    /**
     * Adds a deployment to this service, which will be used when {@link #deployResources()} or {@link #deployResources(boolean)} is called.
     *
     * @param serviceDeployment the deployment to be added to the list of deployments of this service
     */
    void addServiceDeployment(ServiceDeployment serviceDeployment);

    /**
     * Gets a queue containing the last messages of this services console. The max size of this queue can be configured in the nodes config.json.
     *
     * @return a queue with the cached messages of this services console
     */
    Queue<String> getCachedLogMessages();

    /**
     * Stops this service if it is running.
     */
    default void stop() {
        this.setCloudServiceLifeCycle(ServiceLifeCycle.STOPPED);
    }

    /**
     * Starts this service if it is prepared or stopped.
     */
    default void start() {
        this.setCloudServiceLifeCycle(ServiceLifeCycle.RUNNING);
    }

    /**
     * Deletes this service if it is not deleted yet. If this service is running, it will be stopped like {@link #kill()} does.
     */
    default void delete() {
        this.setCloudServiceLifeCycle(ServiceLifeCycle.DELETED);
    }

    /**
     * Sets the life cycle of this service and starts, prepares, stops or deletes this service.
     *
     * @param lifeCycle the lifeCycle to be set
     */
    void setCloudServiceLifeCycle(ServiceLifeCycle lifeCycle);

    /**
     * Stops this service like {@link #stop()} and starts it after it like {@link #start()}.
     */
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

    /**
     * Adds a service template to this service. This template won't be copied directly after adding it but when the service is prepared
     *
     * @param serviceTemplate the template to be added to the list of templates of this service
     */
    ITask<Void> addServiceTemplateAsync(ServiceTemplate serviceTemplate);

    /**
     * Adds a remote inclusion to this service. This remote inclusion won't be included directly after adding it but when the service is prepared
     *
     * @param serviceRemoteInclusion the inclusion to be added to the list of inclusions of this service
     */
    ITask<Void> addServiceRemoteInclusionAsync(ServiceRemoteInclusion serviceRemoteInclusion);

    /**
     * Adds a deployment to this service, which will be used when {@link #deployResources()} or {@link #deployResources(boolean)} is called
     *
     * @param serviceDeployment the deployment to be added to the list of deployments of this service
     */
    ITask<Void> addServiceDeploymentAsync(ServiceDeployment serviceDeployment);

    /**
     * Gets a queue containing the last messages of this services console. The max size of this queue can be configured in the nodes config.json.
     *
     * @return a queue with the cached messages of this services console
     */
    ITask<Queue<String>> getCachedLogMessagesAsync();

    /**
     * Stops this service if it is running.
     */
    default ITask<Void> stopAsync() {
        return this.setCloudServiceLifeCycleAsync(ServiceLifeCycle.STOPPED);
    }

    /**
     * Starts this service if it is prepared or stopped.
     */
    default ITask<Void> startAsync() {
        return this.setCloudServiceLifeCycleAsync(ServiceLifeCycle.RUNNING);
    }

    /**
     * Deletes this service if it is not deleted yet. If this service is running, it will be stopped like {@link #kill()} does.
     */
    default ITask<Void> deleteAsync() {
        return this.setCloudServiceLifeCycleAsync(ServiceLifeCycle.DELETED);
    }

    /**
     * Sets the life cycle of this service and starts, prepares, stops or deletes this service
     *
     * @param lifeCycle the lifeCycle to be set
     */
    ITask<Void> setCloudServiceLifeCycleAsync(ServiceLifeCycle lifeCycle);

    /**
     * Stops this service like {@link #stop()} and starts it after it like {@link #start()}.
     */
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
