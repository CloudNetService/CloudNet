package de.dytanic.cloudnet.driver.provider.service;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * This class provides methods to create and prepare services in the network.
 */
public interface CloudServiceFactory {

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param serviceTask the task the service should be created from
     * @return the info of the created service or null if the service couldn't be created
     */
    @Nullable
    ServiceInfoSnapshot createCloudService(ServiceTask serviceTask);

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param serviceTask the task the service should be created from
     * @param taskId      the id of the service
     * @return the info of the created service or null if the service couldn't be created
     */
    @Nullable
    ServiceInfoSnapshot createCloudService(ServiceTask serviceTask, int taskId);

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param serviceConfiguration the configuration for the new service
     * @return the info of the created service or null if the service couldn't be created
     */
    @Nullable
    ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration);

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param name                 the name of the task of the new cloud service (the name of the service will be name-id)
     * @param runtime              the runtime of the new cloud service (normally this is "jvm")
     * @param autoDeleteOnStop     should the service be automatically deleted on stop?
     * @param staticService        should the service be a static one?
     * @param includes             the additional files that should be included when preparing the service
     * @param templates            the templates that should be included when preparing the service
     * @param deployments          the deployments that should be added to the service by default
     * @param groups               the groups for the service
     * @param processConfiguration the process configuration for the service
     * @param port                 the port of the service
     * @return the info of the created service or null if the service couldn't be created
     */
    @Nullable
    default ServiceInfoSnapshot createCloudService(String name,
                                                   String runtime,
                                                   boolean autoDeleteOnStop,
                                                   boolean staticService,
                                                   Collection<ServiceRemoteInclusion> includes,
                                                   Collection<ServiceTemplate> templates,
                                                   Collection<ServiceDeployment> deployments,
                                                   Collection<String> groups,
                                                   ProcessConfiguration processConfiguration,
                                                   Integer port) {
        return this.createCloudService(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param name                 the name of the task of the new cloud service (the name of the service will be name-id)
     * @param runtime              the runtime of the new cloud service (normally this is "jvm")
     * @param autoDeleteOnStop     should the service be automatically deleted on stop?
     * @param staticService        should the service be a static one?
     * @param includes             the additional files that should be included when preparing the service
     * @param templates            the templates that should be included when preparing the service
     * @param deployments          the deployments that should be added to the service by default
     * @param groups               the groups for the service
     * @param processConfiguration the process configuration for the service
     * @param properties           the properties for the service (those are not used by the cloud, you can define whatever you want as the properties)
     * @param port                 the port of the service
     * @return the info of the created service or null if the service couldn't be created
     */
    @Nullable
    ServiceInfoSnapshot createCloudService(
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            JsonDocument properties,
            Integer port
    );

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param nodeUniqueId         the uniqueId of the node where the services should be started on
     * @param amount               the amount of services to be created
     * @param name                 the name of the task of the new cloud services (the name of the service will be name-id)
     * @param runtime              the runtime of the new cloud services (normally this is "jvm")
     * @param autoDeleteOnStop     should the services be automatically deleted on stop?
     * @param staticService        should the services be a static one?
     * @param includes             the additional files that should be included when preparing the services
     * @param templates            the templates that should be included when preparing the services
     * @param deployments          the deployments that should be added to the services by default
     * @param groups               the groups for the services
     * @param processConfiguration the process configuration for the services
     * @param port                 the port of the services
     * @return the info of the created service or null if the service couldn't be created
     */
    @Nullable
    default Collection<ServiceInfoSnapshot> createCloudService(String nodeUniqueId,
                                                               int amount,
                                                               String name,
                                                               String runtime,
                                                               boolean autoDeleteOnStop,
                                                               boolean staticService,
                                                               Collection<ServiceRemoteInclusion> includes,
                                                               Collection<ServiceTemplate> templates,
                                                               Collection<ServiceDeployment> deployments,
                                                               Collection<String> groups,
                                                               ProcessConfiguration processConfiguration,
                                                               Integer port) {
        return this.createCloudService(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param nodeUniqueId         the uniqueId of the node where the services should be started on
     * @param amount               the amount of services to be created
     * @param name                 the name of the task of the new cloud services (the name of the service will be name-id)
     * @param runtime              the runtime of the new cloud services (normally this is "jvm")
     * @param autoDeleteOnStop     should the services be automatically deleted on stop?
     * @param staticService        should the services be a static one?
     * @param includes             the additional files that should be included when preparing the services
     * @param templates            the templates that should be included when preparing the services
     * @param deployments          the deployments that should be added to the services by default
     * @param groups               the groups for the services
     * @param processConfiguration the process configuration for the services
     * @param properties           the properties for the services (those are not used by the cloud, you can define whatever you want as the properties)
     * @param port                 the port of the services
     * @return the info of the created service or null if the service couldn't be created
     */
    @Nullable
    Collection<ServiceInfoSnapshot> createCloudService(
            String nodeUniqueId,
            int amount,
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            JsonDocument properties,
            Integer port
    );

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param serviceTask the task the service should be created from
     * @return the info of the created service or null if the service couldn't be created
     */
    @NotNull
    ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask);

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param serviceTask the task the service should be created from
     * @param taskId      the id of the service
     * @return the info of the created service or null if the service couldn't be created
     */
    @NotNull
    ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask, int taskId);

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param serviceConfiguration the configuration for the new service
     * @return the info of the created service or null if the service couldn't be created
     */
    @NotNull
    ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration);

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param name                 the name of the task of the new cloud service (the name of the service will be name-id)
     * @param runtime              the runtime of the new cloud service (normally this is "jvm")
     * @param autoDeleteOnStop     should the service be automatically deleted on stop?
     * @param staticService        should the service be a static one?
     * @param includes             the additional files that should be included when preparing the service
     * @param templates            the templates that should be included when preparing the service
     * @param deployments          the deployments that should be added to the service by default
     * @param groups               the groups for the service
     * @param processConfiguration the process configuration for the service
     * @param port                 the port of the service
     * @return the info of the created service or null if the service couldn't be created
     */
    @NotNull
    ITask<ServiceInfoSnapshot> createCloudServiceAsync(String name,
                                                       String runtime,
                                                       boolean autoDeleteOnStop,
                                                       boolean staticService,
                                                       Collection<ServiceRemoteInclusion> includes,
                                                       Collection<ServiceTemplate> templates,
                                                       Collection<ServiceDeployment> deployments,
                                                       Collection<String> groups,
                                                       ProcessConfiguration processConfiguration,
                                                       JsonDocument properties,
                                                       Integer port);

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param name                 the name of the task of the new cloud service (the name of the service will be name-id)
     * @param runtime              the runtime of the new cloud service (normally this is "jvm")
     * @param autoDeleteOnStop     should the service be automatically deleted on stop?
     * @param staticService        should the service be a static one?
     * @param includes             the additional files that should be included when preparing the service
     * @param templates            the templates that should be included when preparing the service
     * @param deployments          the deployments that should be added to the service by default
     * @param groups               the groups for the service
     * @param processConfiguration the process configuration for the service
     * @param properties           the properties for the service (those are not used by the cloud, you can define whatever you want as the properties)
     * @param port                 the port of the service
     * @return the info of the created service or null if the service couldn't be created
     */
    @NotNull
    ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(String nodeUniqueId,
                                                                   int amount,
                                                                   String name,
                                                                   String runtime,
                                                                   boolean autoDeleteOnStop,
                                                                   boolean staticService,
                                                                   Collection<ServiceRemoteInclusion> includes,
                                                                   Collection<ServiceTemplate> templates,
                                                                   Collection<ServiceDeployment> deployments,
                                                                   Collection<String> groups,
                                                                   ProcessConfiguration processConfiguration,
                                                                   JsonDocument properties,
                                                                   Integer port);

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param name                 the name of the task of the new cloud services (the name of the service will be name-id)
     * @param runtime              the runtime of the new cloud services (normally this is "jvm")
     * @param autoDeleteOnStop     should the services be automatically deleted on stop?
     * @param staticService        should the services be a static one?
     * @param includes             the additional files that should be included when preparing the services
     * @param templates            the templates that should be included when preparing the services
     * @param deployments          the deployments that should be added to the services by default
     * @param groups               the groups for the services
     * @param processConfiguration the process configuration for the services
     * @param port                 the port of the services
     * @return the info of the created service or null if the service couldn't be created
     */
    @NotNull
    default ITask<ServiceInfoSnapshot> createCloudServiceAsync(String name,
                                                               String runtime,
                                                               boolean autoDeleteOnStop,
                                                               boolean staticService,
                                                               Collection<ServiceRemoteInclusion> includes,
                                                               Collection<ServiceTemplate> templates,
                                                               Collection<ServiceDeployment> deployments,
                                                               Collection<String> groups,
                                                               ProcessConfiguration processConfiguration,
                                                               Integer port) {
        return this.createCloudServiceAsync(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    /**
     * Creates and prepares (= copies the templates) a new cloud service
     *
     * @param nodeUniqueId         the uniqueId of the node where the services should be started on
     * @param amount               the amount of services to be created
     * @param name                 the name of the task of the new cloud services (the name of the service will be name-id)
     * @param runtime              the runtime of the new cloud services (normally this is "jvm")
     * @param autoDeleteOnStop     should the services be automatically deleted on stop?
     * @param staticService        should the services be a static one?
     * @param includes             the additional files that should be included when preparing the services
     * @param templates            the templates that should be included when preparing the services
     * @param deployments          the deployments that should be added to the services by default
     * @param groups               the groups for the services
     * @param processConfiguration the process configuration for the services
     * @param port                 the port of the services
     * @return the info of the created service or null if the service couldn't be created
     */
    @NotNull
    default ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(String nodeUniqueId,
                                                                           int amount,
                                                                           String name,
                                                                           String runtime,
                                                                           boolean autoDeleteOnStop,
                                                                           boolean staticService,
                                                                           Collection<ServiceRemoteInclusion> includes,
                                                                           Collection<ServiceTemplate> templates,
                                                                           Collection<ServiceDeployment> deployments,
                                                                           Collection<String> groups,
                                                                           ProcessConfiguration processConfiguration,
                                                                           Integer port) {
        return this.createCloudServiceAsync(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

}
