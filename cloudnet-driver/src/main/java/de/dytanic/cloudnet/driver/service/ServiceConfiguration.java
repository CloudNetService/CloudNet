package de.dytanic.cloudnet.driver.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

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

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeObject(this.serviceId);
        buffer.writeString(this.runtime);
        buffer.writeBoolean(this.autoDeleteOnStop);
        buffer.writeBoolean(this.staticService);
        buffer.writeStringCollection(Arrays.asList(this.groups));

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

        buffer.writeStringCollection(Arrays.asList(this.deletedFilesAfterStop));
        buffer.writeObject(this.processConfig);
        buffer.writeInt(this.port);

        super.write(buffer);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.serviceId = buffer.readObject(ServiceId.class);
        this.runtime = buffer.readString();
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

    public static class Builder {

        private final ServiceConfiguration config;

        private Builder() {
            this.config = new ServiceConfiguration();
            this.config.serviceId = new ServiceId();
            this.config.processConfig = new ProcessConfiguration();
            this.config.port = 44955;
        }

        public Builder task(ServiceTask task) {
            return this
                    .task(task.getName())
                    .runtime(task.getRuntime())

                    .autoDeleteOnStop(task.isAutoDeleteOnStop())
                    .staticService(task.isStaticServices())

                    .allowedNodes(task.getAssociatedNodes())
                    .groups(task.getGroups())
                    .deleteFilesAfterStop(task.getDeletedFilesAfterStop())

                    .environment(task.getProcessConfiguration().getEnvironment())
                    .maxHeapMemory(task.getProcessConfiguration().getMaxHeapMemorySize())
                    .jvmOptions(task.getProcessConfiguration().getJvmOptions())
                    .startPort(task.getStartPort());
        }

        public Builder serviceId(ServiceId serviceId) {
            this.config.serviceId = serviceId;
            return this;
        }

        public Builder task(String task) {
            this.config.serviceId.taskName = task;
            return this;
        }

        public Builder environment(ServiceEnvironmentType environment) {
            this.config.serviceId.environment = environment;
            this.config.processConfig.environment = environment;
            return this;
        }

        public Builder taskId(int taskId) {
            this.config.serviceId.taskServiceId = taskId;
            return this;
        }

        public Builder uniqueId(UUID uniqueId) {
            this.config.serviceId.uniqueId = uniqueId;
            return this;
        }

        public Builder node(String nodeUniqueId) {
            this.config.serviceId.nodeUniqueId = nodeUniqueId;
            return this;
        }

        public Builder allowedNodes(Collection<String> allowedNodes) {
            this.config.serviceId.allowedNodes = allowedNodes;
            return this;
        }

        public Builder allowedNodes(String... allowedNodes) {
            return this.allowedNodes(Arrays.asList(allowedNodes));
        }

        public Builder runtime(String runtime) {
            this.config.runtime = runtime;
            return this;
        }

        public Builder autoDeleteOnStop(boolean autoDeleteOnStop) {
            this.config.autoDeleteOnStop = autoDeleteOnStop;
            return this;
        }

        public Builder autoDeleteOnStop() {
            return this.autoDeleteOnStop(true);
        }

        public Builder staticService(boolean staticService) {
            this.config.staticService = staticService;
            return this;
        }

        public Builder staticService() {
            return this.staticService(true);
        }

        public Builder groups(String... groups) {
            this.config.groups = groups;
            return this;
        }

        public Builder groups(Collection<String> groups) {
            return this.groups(groups.toArray(new String[0]));
        }

        public Builder inclusions(ServiceRemoteInclusion... inclusions) {
            this.config.includes = inclusions;
            return this;
        }

        public Builder inclusions(Collection<ServiceRemoteInclusion> inclusions) {
            return this.inclusions(inclusions.toArray(new ServiceRemoteInclusion[0]));
        }

        public Builder templates(ServiceTemplate... templates) {
            this.config.templates = templates;
            return this;
        }

        public Builder templates(Collection<ServiceTemplate> templates) {
            return this.templates(templates.toArray(new ServiceTemplate[0]));
        }

        public Builder deployments(ServiceDeployment... deployments) {
            this.config.deployments = deployments;
            return this;
        }

        public Builder deployments(Collection<ServiceDeployment> deployments) {
            return this.deployments(deployments.toArray(new ServiceDeployment[0]));
        }

        public Builder deleteFilesAfterStop(String... deletedFilesAfterStop) {
            this.config.deletedFilesAfterStop = deletedFilesAfterStop;
            return this;
        }

        public Builder deleteFilesAfterStop(Collection<String> deletedFilesAfterStop) {
            return this.deleteFilesAfterStop(deletedFilesAfterStop.toArray(new String[0]));
        }

        public Builder maxHeapMemory(int maxHeapMemory) {
            this.config.processConfig.setMaxHeapMemorySize(maxHeapMemory);
            return this;
        }

        public Builder jvmOptions(Collection<String> jvmOptions) {
            this.config.processConfig.jvmOptions = jvmOptions;
            return this;
        }

        public Builder jvmOptions(String... jvmOptions) {
            return this.jvmOptions(Arrays.asList(jvmOptions));
        }

        public Builder startPort(int startPort) {
            this.config.port = startPort;
            return this;
        }

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