package de.dytanic.cloudnet.driver.provider.service;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

import java.util.Collection;
import java.util.UUID;

public interface GeneralCloudServiceProvider {

    /**
     * Gets a list with the uniqueIds of all services in the cloud
     *
     * @return a list containing the uniqueIds of every service in the whole cloud
     */
    Collection<UUID> getServicesAsUniqueId();

    /**
     * Gets a list with the infos of all services in the cloud
     *
     * @return a list containing the infos of every service in the whole cloud
     */
    Collection<ServiceInfoSnapshot> getCloudServices();

    /**
     * Gets a list with the infos of all started services in the cloud
     *
     * @return a list containing the infos of every started service in the whole cloud
     */
    Collection<ServiceInfoSnapshot> getStartedCloudServices();

    /**
     * Gets a list with the infos of all services in the cloud that are from the given task
     *
     * @param taskName the name of the task every service in the list should have
     * @return a list containing the infos of every service with the given task in the whole cloud
     */
    Collection<ServiceInfoSnapshot> getCloudServices(String taskName);


    /**
     * Gets a list with the infos of all services in the cloud that have the given environment
     *
     * @param environment the environment every service in the list should have
     * @return a list containing the infos of every service with the given environment in the whole cloud
     */
    Collection<ServiceInfoSnapshot> getCloudServices(ServiceEnvironmentType environment);

    /**
     * Gets a list with the infos of all services in the cloud that have the given group
     *
     * @param group the name of the task every service in the list should have
     * @return a list containing the infos of every service with the given group in the whole cloud
     */
    Collection<ServiceInfoSnapshot> getCloudServicesByGroup(String group);

    /**
     * Gets the amount of services in the cloud
     *
     * @return an integer for the amount of services in the whole cloud
     */
    int getServicesCount();

    /**
     * Gets the amount of services by the given group in the cloud
     *
     * @param group the group every service counting should have
     * @return an integer for the amount of services in the whole cloud
     */
    int getServicesCountByGroup(String group);

    /**
     * Gets the amount of services by the given task in the cloud
     *
     * @param taskName the task every service counting should have
     * @return an integer for the amount of services in the whole cloud
     */
    int getServicesCountByTask(String taskName);

    /**
     * Gets the info of a cloud service by its name
     *
     * @param name the name of the service
     * @return the info of the service or {@code null} if the service doesn't exist
     */
    ServiceInfoSnapshot getCloudServiceByName(String name);

    /**
     * Gets the info of a cloud service by its uniqueId
     *
     * @param uniqueId the uniqueId of the service
     * @return the info of the service or {@code null} if the service doesn't exist
     */
    ServiceInfoSnapshot getCloudService(UUID uniqueId);



    /**
     * Gets a list with the uniqueIds of all services in the cloud
     *
     * @return a list containing the uniqueIds of every service in the whole cloud
     */
    ITask<Collection<UUID>> getServicesAsUniqueIdAsync();

    /**
     * Gets a list with the infos of all services in the cloud
     *
     * @return a list containing the infos of every service in the whole cloud
     */
    ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync();

    /**
     * Gets a list with the infos of all started services in the cloud
     *
     * @return a list containing the infos of every started service in the whole cloud
     */
    ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServicesAsync();

    /**
     * Gets a list with the infos of all services in the cloud that are from the given task
     *
     * @param taskName the name of the task every service in the list should have
     * @return a list containing the infos of every service with the given task in the whole cloud
     */
    ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName);

    /**
     * Gets a list with the infos of all services in the cloud that have the given environment
     *
     * @param environment the environment every service in the list should have
     * @return a list containing the infos of every service with the given environment in the whole cloud
     */
    ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(ServiceEnvironmentType environment);

    /**
     * Gets a list with the infos of all services in the cloud that have the given group
     *
     * @param group the name of the task every service in the list should have
     * @return a list containing the infos of every service with the given group in the whole cloud
     */
    ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group);

    /**
     * Gets the amount of services in the cloud
     *
     * @return an integer for the amount of services in the whole cloud
     */
    ITask<Integer> getServicesCountAsync();

    /**
     * Gets the amount of services by the given group in the cloud
     *
     * @param group the group every service counting should have
     * @return an integer for the amount of services in the whole cloud
     */
    ITask<Integer> getServicesCountByGroupAsync(String group);

    /**
     * Gets the amount of services by the given task in the cloud
     *
     * @param taskName the task every service counting should have
     * @return an integer for the amount of services in the whole cloud
     */
    ITask<Integer> getServicesCountByTaskAsync(String taskName);

    /**
     * Gets the info of a cloud service by its name
     *
     * @param name the name of the service
     * @return the info of the service or {@code null} if the service doesn't exist
     */
    ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name);

    /**
     * Gets the info of a cloud service by its uniqueId
     *
     * @param uniqueId the uniqueId of the service
     * @return the info of the service or {@code null} if the service doesn't exist
     */
    ITask<ServiceInfoSnapshot> getCloudServiceAsync(UUID uniqueId);

}
