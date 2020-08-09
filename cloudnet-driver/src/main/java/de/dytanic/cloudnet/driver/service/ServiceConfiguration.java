package de.dytanic.cloudnet.driver.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceConfiguration extends SerializableJsonDocPropertyable implements SerializableObject {

    protected ServiceId serviceId;

    protected String runtime;

    protected boolean autoDeleteOnStop, staticService;

    protected String[] groups;

    protected ServiceRemoteInclusion[] includes;
    protected ServiceTemplate[] templates;
    protected ServiceDeployment[] deployments;

    private ServiceRemoteInclusion[] initIncludes;
    private ServiceTemplate[] initTemplates;
    private ServiceDeployment[] initDeployments;

    protected String[] deletedFilesAfterStop;

    protected ProcessConfiguration processConfig;

    protected int port;

    public ServiceConfiguration(ServiceId serviceId, String runtime, boolean autoDeleteOnStop, boolean staticService, String[] groups, ServiceRemoteInclusion[] includes, ServiceTemplate[] templates, ServiceDeployment[] deployments, ProcessConfiguration processConfig, int port) {
        this(serviceId, runtime, autoDeleteOnStop, staticService, groups, includes, templates, deployments, new String[0], processConfig, port);
    }

    public ServiceConfiguration(ServiceId serviceId, String runtime, boolean autoDeleteOnStop, boolean staticService, String[] groups, ServiceRemoteInclusion[] includes, ServiceTemplate[] templates, ServiceDeployment[] deployments, String[] deletedFilesAfterStop, ProcessConfiguration processConfig, int port) {
        this(serviceId, runtime, autoDeleteOnStop, staticService, groups, includes, templates, deployments, deletedFilesAfterStop, processConfig, JsonDocument.newDocument(), port);
    }

    public ServiceConfiguration(ServiceId serviceId, String runtime, boolean autoDeleteOnStop, boolean staticService, String[] groups, ServiceRemoteInclusion[] includes, ServiceTemplate[] templates, ServiceDeployment[] deployments, ProcessConfiguration processConfig, JsonDocument properties, int port) {
        this(serviceId, runtime, autoDeleteOnStop, staticService, groups, includes, templates, deployments, new String[0], processConfig, properties, port);
    }

    public ServiceConfiguration(ServiceId serviceId, String runtime, boolean autoDeleteOnStop, boolean staticService, String[] groups, ServiceRemoteInclusion[] includes, ServiceTemplate[] templates, ServiceDeployment[] deployments, String[] deletedFilesAfterStop, ProcessConfiguration processConfig, JsonDocument properties, int port) {
        this.serviceId = serviceId;
        this.runtime = runtime;
        this.autoDeleteOnStop = autoDeleteOnStop;
        this.staticService = staticService;
        this.groups = groups;
        this.includes = this.initIncludes = includes;
        this.templates = this.initTemplates = templates;
        this.deployments = this.initDeployments = deployments;
        this.deletedFilesAfterStop = deletedFilesAfterStop;
        this.processConfig = processConfig;
        this.properties = properties;
        this.port = port;
    }

    public ServiceConfiguration() {
    }

    public ServiceId getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(ServiceId serviceId) {
        this.serviceId = serviceId;
    }

    public String getRuntime() {
        return this.runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public boolean isAutoDeleteOnStop() {
        return this.autoDeleteOnStop;
    }

    public void setAutoDeleteOnStop(boolean autoDeleteOnStop) {
        this.autoDeleteOnStop = autoDeleteOnStop;
    }

    public boolean isStaticService() {
        return this.staticService;
    }

    public void setStaticService(boolean staticService) {
        this.staticService = staticService;
    }

    public String[] getGroups() {
        return this.groups;
    }

    public boolean hasGroup(String group) {
        for (String s : this.groups) {
            if (s.equalsIgnoreCase(group)) {
                return true;
            }
        }
        return false;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    public ServiceRemoteInclusion[] getIncludes() {
        return this.includes;
    }

    public void setIncludes(ServiceRemoteInclusion[] includes) {
        this.includes = includes;
    }

    public ServiceTemplate[] getTemplates() {
        return this.templates;
    }

    public void setTemplates(ServiceTemplate[] templates) {
        this.templates = templates;
    }

    public ServiceDeployment[] getDeployments() {
        return this.deployments;
    }

    public void setDeployments(ServiceDeployment[] deployments) {
        this.deployments = deployments;
    }

    public String[] getDeletedFilesAfterStop() {
        return this.deletedFilesAfterStop;
    }

    public void setDeletedFilesAfterStop(String[] deletedFilesAfterStop) {
        this.deletedFilesAfterStop = deletedFilesAfterStop;
    }

    public ProcessConfiguration getProcessConfig() {
        return this.processConfig;
    }

    public void setProcessConfig(ProcessConfiguration processConfig) {
        this.processConfig = processConfig;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setInitDeployments(ServiceDeployment[] initDeployments) {
        this.initDeployments = initDeployments;
    }

    public void setInitIncludes(ServiceRemoteInclusion[] initIncludes) {
        this.initIncludes = initIncludes;
    }

    public void setInitTemplates(ServiceTemplate[] initTemplates) {
        this.initTemplates = initTemplates;
    }

    public ServiceDeployment[] getInitDeployments() {
        return this.initDeployments;
    }

    public ServiceRemoteInclusion[] getInitIncludes() {
        return this.initIncludes;
    }

    public ServiceTemplate[] getInitTemplates() {
        return this.initTemplates;
    }

    @Nullable
    public ServiceInfoSnapshot createNewService() {
        return CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(this);
    }

    @NotNull
    public ITask<ServiceInfoSnapshot> createNewServiceAsync() {
        return CloudNetDriver.getInstance().getCloudServiceFactory().createCloudServiceAsync(this);
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeObject(this.serviceId);
        buffer.writeOptionalString(this.runtime);
        buffer.writeBoolean(this.autoDeleteOnStop);
        buffer.writeBoolean(this.staticService);
        buffer.writeStringCollection(this.groups == null ? Collections.emptyList() : Arrays.asList(this.groups));

        buffer.writeObjectArray(this.includes);
        buffer.writeObjectArray(this.templates);
        buffer.writeObjectArray(this.deployments);

        buffer.writeBoolean(this.initIncludes != null);
        if (this.initIncludes != null) {
            buffer.writeObjectArray(this.initIncludes);
        }
        buffer.writeBoolean(this.initTemplates != null);
        if (this.initTemplates != null) {
            buffer.writeObjectArray(this.initTemplates);
        }
        buffer.writeBoolean(this.initDeployments != null);
        if (this.initDeployments != null) {
            buffer.writeObjectArray(this.initDeployments);
        }

        buffer.writeStringCollection(this.deletedFilesAfterStop == null ? Collections.emptyList() : Arrays.asList(this.deletedFilesAfterStop));
        buffer.writeObject(this.processConfig);
        buffer.writeInt(this.port);

        super.write(buffer);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.serviceId = buffer.readObject(ServiceId.class);
        this.runtime = buffer.readOptionalString();
        this.autoDeleteOnStop = buffer.readBoolean();
        this.staticService = buffer.readBoolean();
        this.groups = buffer.readStringCollection().toArray(new String[0]);

        this.includes = buffer.readObjectArray(ServiceRemoteInclusion.class);
        this.templates = buffer.readObjectArray(ServiceTemplate.class);
        this.deployments = buffer.readObjectArray(ServiceDeployment.class);
        this.initIncludes = buffer.readBoolean() ? buffer.readObjectArray(ServiceRemoteInclusion.class) : null;
        this.initTemplates = buffer.readBoolean() ? buffer.readObjectArray(ServiceTemplate.class) : null;
        this.initDeployments = buffer.readBoolean() ? buffer.readObjectArray(ServiceDeployment.class) : null;

        this.deletedFilesAfterStop = buffer.readStringCollection().toArray(new String[0]);
        this.processConfig = buffer.readObject(ProcessConfiguration.class);
        this.port = buffer.readInt();

        super.read(buffer);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ServiceTask task) {
        return builder().task(task);
    }

    /**
     * Builder for the creation of new services.
     * All required parameters are:
     * <ul>
     *     <li>{@link #task(String)}</li>
     *     <li>{@link #environment(ServiceEnvironmentType)}</li>
     *     <li>{@link #maxHeapMemory(int)}</li>
     * </ul>
     * You can create a new service with this example:
     * <p>
     * {@code ServiceConfiguration.builder().task("Lobby").environment(ServiceEnvironmentType.MINECRAFT_SERVER).maxHeapMemory(512).build().createNewService()}
     * <p>
     * this will return the newly created {@link ServiceInfoSnapshot} or null if the service couldn't be created.
     */
    public static class Builder {

        private final ServiceConfiguration config;

        private Builder() {
            this.config = new ServiceConfiguration();
            this.config.serviceId = new ServiceId();
            this.config.processConfig = new ProcessConfiguration();
            this.config.port = 44955;
        }

        /**
         * Applies every option of the given {@link ServiceTask} object except for the Properties.
         * This will override every previously set option of this builder.
         */
        public Builder task(ServiceTask task) {
            return this
                    .task(task.getName())
                    .runtime(task.getRuntime())

                    .autoDeleteOnStop(task.isAutoDeleteOnStop())
                    .staticService(task.isStaticServices())

                    .allowedNodes(task.getAssociatedNodes())
                    .groups(task.getGroups())
                    .deleteFilesAfterStop(task.getDeletedFilesAfterStop())

                    .templates(task.getTemplates())
                    .deployments(task.getDeployments())
                    .inclusions(task.getIncludes())

                    .environment(task.getProcessConfiguration().getEnvironment())
                    .maxHeapMemory(task.getProcessConfiguration().getMaxHeapMemorySize())
                    .jvmOptions(task.getProcessConfiguration().getJvmOptions())
                    .processParameters(task.getProcessConfiguration().getProcessParameters())
                    .startPort(task.getStartPort());
        }

        /**
         * The complete {@link ServiceId} for the new service.
         * Calling this method will override all the following method calls:
         * <ul>
         *     <li>{@link #task(String)}</li>
         *     <li>{@link #taskId(int)}</li>
         *     <li>{@link #uniqueId(UUID)}</li>
         *     <li>{@link #environment(ServiceEnvironmentType)}</li>
         *     <li>{@link #node(String)}</li>
         *     <li>{@link #allowedNodes(String...)} / {@link #allowedNodes(Collection)}</li>
         * </ul>
         */
        public Builder serviceId(ServiceId serviceId) {
            this.config.serviceId = serviceId;
            return this;
        }

        /**
         * The task for the new service. No permanent task with that name has to exist.
         * This will NOT use any options of the given task, to do that use {@link #task(ServiceTask)}.
         */
        public Builder task(String task) {
            this.config.serviceId.taskName = task;
            return this;
        }

        /**
         * The environment for the new service.
         */
        public Builder environment(ServiceEnvironmentType environment) {
            this.config.serviceId.environment = environment;
            this.config.processConfig.environment = environment;
            return this;
        }

        /**
         * The task id for the new service (For example Lobby-1 would have the task id 1).
         */
        public Builder taskId(int taskId) {
            this.config.serviceId.taskServiceId = taskId;
            return this;
        }

        /**
         * The uniqueId for the new service.
         */
        public Builder uniqueId(UUID uniqueId) {
            this.config.serviceId.uniqueId = uniqueId;
            return this;
        }

        /**
         * The node where the new service will start. If the service cannot be created on this node or the node doesn't exist, it
         * will NOT be created and {@link ServiceConfiguration#createNewService()} will return {@code null}.
         */
        public Builder node(String nodeUniqueId) {
            this.config.serviceId.nodeUniqueId = nodeUniqueId;
            return this;
        }

        /**
         * A list of all allowed nodes. CloudNet will choose the node with the most free resources.
         * If a node is provided using {@link #node(String)}, this option will be ignored.
         */
        public Builder allowedNodes(Collection<String> allowedNodes) {
            this.config.serviceId.allowedNodes = new ArrayList<>(allowedNodes);
            return this;
        }

        /**
         * A list of all allowed nodes. CloudNet will choose the node with the most free resources.
         * If a node is provided using {@link #node(String)}, this option will be ignored.
         */
        public Builder allowedNodes(String... allowedNodes) {
            return this.allowedNodes(Arrays.asList(allowedNodes));
        }

        /**
         * A list of all allowed nodes. CloudNet will choose the node with the most free resources.
         * If a node is provided using {@link #node(String)}, this option will be ignored.
         */
        public Builder addAllowedNodes(Collection<String> allowedNodes) {
            if(this.config.serviceId.allowedNodes == null) {
                return this.allowedNodes(allowedNodes);
            }
            this.config.serviceId.allowedNodes.addAll(allowedNodes);
            return this;
        }

        /**
         * A list of all allowed nodes. CloudNet will choose the node with the most free resources.
         * If a node is provided using {@link #node(String)}, this option will be ignored.
         */
        public Builder addAllowedNodes(String... allowedNodes) {
            return this.addAllowedNodes(Arrays.asList(allowedNodes));
        }

        /**
         * The runtime of the service. If none is provided, the default "jvm" is used.
         * By default, CloudNet only provides the "jvm" runtime, you can add your own with custom modules.
         */
        public Builder runtime(String runtime) {
            this.config.runtime = runtime;
            return this;
        }

        /**
         * Whether this service should be deleted on stop (doesn't affect files of a static service) or the life cycle
         * should be changed to {@link ServiceLifeCycle#PREPARED}.
         */
        public Builder autoDeleteOnStop(boolean autoDeleteOnStop) {
            this.config.autoDeleteOnStop = autoDeleteOnStop;
            return this;
        }

        /**
         * Alias for {@code autoDeleteOnStop(true)}.
         */
        public Builder autoDeleteOnStop() {
            return this.autoDeleteOnStop(true);
        }

        /**
         * Whether the files should be deleted or saved on deletion of the service.
         */
        public Builder staticService(boolean staticService) {
            this.config.staticService = staticService;
            return this;
        }

        /**
         * Alias for {@code staticService(true)}.
         */
        public Builder staticService() {
            return this.staticService(true);
        }

        /**
         * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
         * to the new service.
         */
        public Builder groups(String... groups) {
            this.config.groups = groups;
            return this;
        }

        /**
         * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
         * to the new service.
         */
        public Builder groups(Collection<String> groups) {
            return this.groups(groups.toArray(new String[0]));
        }

        /**
         * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
         * to the new service.
         */
        public Builder addGroups(String... groups) {
            List<String> groupList = Arrays.asList(groups);
            groupList.addAll(Arrays.asList(this.config.groups));
            this.config.groups = groupList.toArray(new String[0]);
            return this;
        }

        /**
         * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
         * to the new service.
         */
        public Builder addGroups(Collection<String> groups) {
            return this.addGroups(groups.toArray(new String[0]));
        }

        /**
         * The inclusions for the new service. They will be copied into the service directory before the service is started
         * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
         */
        public Builder inclusions(ServiceRemoteInclusion... inclusions) {
            this.config.includes = inclusions;
            return this;
        }

        /**
         * The inclusions for the new service. They will be copied into the service directory before the service is started
         * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
         */
        public Builder inclusions(Collection<ServiceRemoteInclusion> inclusions) {
            return this.inclusions(inclusions.toArray(new ServiceRemoteInclusion[0]));
        }

        /**
         * The inclusions for the new service. They will be copied into the service directory before the service is started
         * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
         */
        public Builder addInclusions(ServiceRemoteInclusion... inclusions) {
            List<ServiceRemoteInclusion> serviceRemoteInclusions = Arrays.asList(inclusions);
            serviceRemoteInclusions.addAll(Arrays.asList(this.config.includes));
            this.config.includes = serviceRemoteInclusions.toArray(new ServiceRemoteInclusion[0]);
            return this;
        }

        /**
         * The inclusions for the new service. They will be copied into the service directory before the service is started
         * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
         */
        public Builder addInclusions(Collection<ServiceRemoteInclusion> inclusions) {
            return this.addInclusions(inclusions.toArray(new ServiceRemoteInclusion[0]));
        }

        /**
         * The templates for the new service. They will be copied into the service directory before the service is started
         * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
         */
        public Builder templates(ServiceTemplate... templates) {
            this.config.templates = templates;
            return this;
        }

        /**
         * The templates for the new service. They will be copied into the service directory before the service is started
         * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
         */
        public Builder templates(Collection<ServiceTemplate> templates) {
            return this.templates(templates.toArray(new ServiceTemplate[0]));
        }

        /**
         * The templates for the new service. They will be copied into the service directory before the service is started
         * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
         */
        public Builder addTemplates(ServiceTemplate... templates) {
            List<ServiceTemplate> serviceTemplates = Arrays.asList(templates);
            serviceTemplates.addAll(Arrays.asList(this.config.templates));
            this.config.templates = serviceTemplates.toArray(new ServiceTemplate[0]);
            return this;
        }

        /**
         * The templates for the new service. They will be copied into the service directory before the service is started
         * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
         */
        public Builder addTemplates(Collection<ServiceTemplate> templates) {
            return this.addTemplates(templates.toArray(new ServiceTemplate[0]));
        }

        /**
         * The deployments for the new service. They will be copied into the template after the service is stopped
         * or by calling {@link SpecificCloudServiceProvider#deployResources()}.
         */
        public Builder deployments(ServiceDeployment... deployments) {
            this.config.deployments = deployments;
            return this;
        }

        /**
         * The deployments for the new service. They will be copied into the template after the service is stopped
         * or by calling {@link SpecificCloudServiceProvider#deployResources()}.
         */
        public Builder deployments(Collection<ServiceDeployment> deployments) {
            return this.deployments(deployments.toArray(new ServiceDeployment[0]));
        }

        /**
         * The deployments for the new service. They will be copied into the template after the service is stopped
         * or by calling {@link SpecificCloudServiceProvider#deployResources()}.
         */
        public Builder addDeployments(ServiceDeployment... deployments) {
            List<ServiceDeployment> serviceDeployments = Arrays.asList(deployments);
            serviceDeployments.addAll(Arrays.asList(this.config.deployments));
            this.config.deployments = serviceDeployments.toArray(new ServiceDeployment[0]);
            return this;
        }

        /**
         * The deployments for the new service. They will be copied into the template after the service is stopped
         * or by calling {@link SpecificCloudServiceProvider#deployResources()}.
         */
        public Builder addDeployments(Collection<ServiceDeployment> deployments) {
            return this.addDeployments(deployments.toArray(new ServiceDeployment[0]));
        }

        /**
         * The files that should be deleted after the service has been stopped.
         */
        public Builder deleteFilesAfterStop(String... deletedFilesAfterStop) {
            this.config.deletedFilesAfterStop = deletedFilesAfterStop;
            return this;
        }

        /**
         * The files that should be deleted after the service has been stopped.
         */
        public Builder deleteFilesAfterStop(Collection<String> deletedFilesAfterStop) {
            return this.deleteFilesAfterStop(deletedFilesAfterStop.toArray(new String[0]));
        }

        /**
         * The files that should be deleted after the service has been stopped.
         */
        public Builder addDeleteFilesAfterStop(String... deletedFilesAfterStop) {
            List<String> deletedFiles = Arrays.asList(deletedFilesAfterStop);
            deletedFiles.addAll(Arrays.asList(this.config.deletedFilesAfterStop));
            this.config.deletedFilesAfterStop = deletedFiles.toArray(new String[0]);
            return this;
        }

        /**
         * The files that should be deleted after the service has been stopped.
         */
        public Builder addDeleteFilesAfterStop(Collection<String> deletedFilesAfterStop) {
            return this.addDeleteFilesAfterStop(deletedFilesAfterStop.toArray(new String[0]));
        }

        /**
         * The max heap memory for the new service.
         */
        public Builder maxHeapMemory(int maxHeapMemory) {
            this.config.processConfig.setMaxHeapMemorySize(maxHeapMemory);
            return this;
        }

        /**
         * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup command.
         */
        public Builder jvmOptions(Collection<String> jvmOptions) {
            this.config.processConfig.jvmOptions = new ArrayList<>(jvmOptions);
            return this;
        }

        /**
         * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup command.
         */
        public Builder jvmOptions(String... jvmOptions) {
            return this.jvmOptions(Arrays.asList(jvmOptions));
        }

        /**
         * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup command.
         */
        public Builder addJvmOptions(String... jvmOptions) {
            return this.addJvmOptions(Arrays.asList(jvmOptions));
        }

        /**
         * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup command.
         */
        public Builder addJvmOptions(Collection<String> jvmOptions) {
            if(this.config.processConfig.jvmOptions == null) {
                return this.jvmOptions(jvmOptions);
            }
            this.config.processConfig.jvmOptions.addAll(jvmOptions);
            return this;
        }

        /**
         * The process parameters for the new service. This will be the last parameters that will be added to the command.
         */
        public Builder processParameters(Collection<String> jvmOptions) {
            this.config.processConfig.processParameters = new ArrayList<>(jvmOptions);
            return this;
        }

        /**
         * The process parameters for the new service. This will be the last parameters that will be added to the command.
         */
        public Builder addProcessParameters(String... jvmOptions) {
            return this.addProcessParameters(Arrays.asList(jvmOptions));
        }

        /**
         * The process parameters for the new service. This will be the last parameters that will be added to the command.
         */
        public Builder addProcessParameters(Collection<String> jvmOptions) {
            this.config.processConfig.processParameters.addAll(jvmOptions);
            return this;
        }

        /**
         * The start port for the new service. CloudNet will test whether the port is used or not, it will count up 1
         * while the port is used.
         */
        public Builder startPort(int startPort) {
            this.config.port = startPort;
            return this;
        }

        /**
         * The default properties of the new service. CloudNet itself completely ignores them, but they can be useful if
         * you want to transport data from the component that has created the service to the new service.
         */
        public Builder properties(JsonDocument properties) {
            this.config.properties = properties;
            return this;
        }

        public ServiceConfiguration build() {
            Preconditions.checkNotNull(this.config.serviceId.taskName, "No task provided");
            Preconditions.checkNotNull(this.config.serviceId.environment, "No environment provided");
            Preconditions.checkArgument(this.config.processConfig.maxHeapMemorySize > 0, "No max heap memory provided");
            Preconditions.checkArgument(this.config.port > 0, "StartPort has to greater than 0");

            if (this.config.templates == null) {
                this.config.templates = new ServiceTemplate[0];
            }
            if (this.config.deployments == null) {
                this.config.deployments = new ServiceDeployment[0];
            }
            if (this.config.includes == null) {
                this.config.includes = new ServiceRemoteInclusion[0];
            }
            if (this.config.serviceId.uniqueId == null) {
                this.config.serviceId.uniqueId = UUID.randomUUID();
            }
            if (this.config.processConfig.jvmOptions == null) {
                this.config.processConfig.jvmOptions = Collections.emptyList();
            }

            return this.config;
        }

    }

}
