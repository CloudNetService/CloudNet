package de.dytanic.cloudnet.wrapper.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.wrapper.DriverAPIUser;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class WrapperCloudServiceFactory implements CloudServiceFactory, DriverAPIUser {

    private final Wrapper wrapper;

    public WrapperCloudServiceFactory(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot createCloudService(ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        return this.createCloudServiceAsync(serviceTask).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public @Nullable ServiceInfoSnapshot createCloudService(ServiceTask serviceTask, int taskId) {
        Preconditions.checkNotNull(serviceTask);

        return this.createCloudServiceAsync(serviceTask, taskId).get(5, TimeUnit.SECONDS, null);
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        Preconditions.checkNotNull(serviceConfiguration);

        return this.createCloudServiceAsync(serviceConfiguration).get(5, TimeUnit.SECONDS, null);
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot createCloudService(String name,
                                                  String runtime,
                                                  boolean autoDeleteOnStop,
                                                  boolean staticService,
                                                  Collection<ServiceRemoteInclusion> includes,
                                                  Collection<ServiceTemplate> templates,
                                                  Collection<ServiceDeployment> deployments,
                                                  Collection<String> groups,
                                                  ProcessConfiguration processConfiguration,
                                                  JsonDocument properties,
                                                  Integer port) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        return this.createCloudServiceAsync(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port).get(5, TimeUnit.SECONDS, null);
    }

    @Nullable
    @Override
    public Collection<ServiceInfoSnapshot> createCloudService(String nodeUniqueId,
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
                                                              Integer port) {
        Preconditions.checkNotNull(nodeUniqueId);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        return this.createCloudServiceAsync(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.CREATE_CLOUD_SERVICE_BY_SERVICE_TASK,
                buffer -> buffer.writeObject(serviceTask),
                packet -> packet.getBuffer().readObject(ServiceInfoSnapshot.class)
        );
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask, int taskId) {
        Preconditions.checkNotNull(serviceTask);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.CREATE_CLOUD_SERVICE_BY_SERVICE_TASK_AND_ID,
                buffer -> buffer.writeObject(serviceTask).writeInt(taskId),
                packet -> packet.getBuffer().readObject(ServiceInfoSnapshot.class)
        );
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
        Preconditions.checkNotNull(serviceConfiguration);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.CREATE_CLOUD_SERVICE_BY_CONFIGURATION,
                buffer -> buffer.writeObject(serviceConfiguration),
                packet -> packet.getBuffer().readObject(ServiceInfoSnapshot.class)
        );
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(String name,
                                                              String runtime,
                                                              boolean autoDeleteOnStop,
                                                              boolean staticService,
                                                              Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates,
                                                              Collection<ServiceDeployment> deployments,
                                                              Collection<String> groups,
                                                              ProcessConfiguration processConfiguration,
                                                              JsonDocument properties,
                                                              Integer port) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.CREATE_CUSTOM_CLOUD_SERVICE,
                buffer -> buffer.writeString(name)
                        .writeString(runtime)
                        .writeBoolean(autoDeleteOnStop)
                        .writeBoolean(staticService)
                        .writeObjectCollection(includes)
                        .writeObjectCollection(deployments)
                        .writeStringCollection(groups)
                        .writeObject(processConfiguration)
                        .writeJsonDocument(properties)
                        .writeInt(port),
                packet -> packet.getBuffer().readObject(ServiceInfoSnapshot.class)
        );
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(String nodeUniqueId,
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
                                                                          Integer port) {
        Preconditions.checkNotNull(nodeUniqueId);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.CREATE_CUSTOM_CLOUD_SERVICE_WITH_NODE_AND_AMOUNT,
                buffer -> buffer.writeString(nodeUniqueId)
                        .writeInt(amount)
                        .writeString(name)
                        .writeString(runtime)
                        .writeBoolean(autoDeleteOnStop)
                        .writeBoolean(staticService)
                        .writeObjectCollection(includes)
                        .writeObjectCollection(deployments)
                        .writeStringCollection(groups)
                        .writeObject(processConfiguration)
                        .writeJsonDocument(properties)
                        .writeInt(port),
                packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
        );
    }

    @Override
    public INetworkClient getNetworkClient() {
        return this.wrapper.getNetworkClient();
    }
}
