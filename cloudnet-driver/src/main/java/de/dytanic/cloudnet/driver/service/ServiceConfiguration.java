package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Arrays;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class ServiceConfiguration extends BasicJsonDocPropertyable implements SerializableObject {

    private ServiceId serviceId;

    private String runtime;

    private boolean autoDeleteOnStop, staticService;

    private String[] groups;

    private ServiceRemoteInclusion[] includes;
    private ServiceTemplate[] templates;
    private ServiceDeployment[] deployments;

    private ServiceRemoteInclusion[] initIncludes;
    private ServiceTemplate[] initTemplates;
    private ServiceDeployment[] initDeployments;

    private String[] deletedFilesAfterStop;

    private ProcessConfiguration processConfig;

    private int port;

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
    public void write(ProtocolBuffer buffer) {
        buffer.writeObject(this.serviceId);
        buffer.writeString(this.runtime);
        buffer.writeBoolean(this.autoDeleteOnStop);
        buffer.writeBoolean(this.staticService);
        buffer.writeStringCollection(Arrays.asList(this.groups));

        buffer.writeObjectArray(this.includes);
        buffer.writeObjectArray(this.templates);
        buffer.writeObjectArray(this.deployments);
        buffer.writeObjectArray(this.initIncludes);
        buffer.writeObjectArray(this.initTemplates);
        buffer.writeObjectArray(this.initDeployments);

        buffer.writeStringCollection(Arrays.asList(this.deletedFilesAfterStop));
        buffer.writeObject(this.processConfig);
        buffer.writeInt(this.port);

        buffer.writeString(super.properties.toJson());
    }

    @Override
    public void read(ProtocolBuffer buffer) {
        buffer.writeObject(this.serviceId);
        this.runtime = buffer.readString();
        this.autoDeleteOnStop = buffer.readBoolean();
        this.staticService = buffer.readBoolean();
        this.groups = buffer.readStringCollection().toArray(new String[0]);

        this.includes = buffer.readObjectArray(ServiceRemoteInclusion.class);
        this.templates = buffer.readObjectArray(ServiceTemplate.class);
        this.deployments = buffer.readObjectArray(ServiceDeployment.class);
        this.initIncludes = buffer.readObjectArray(ServiceRemoteInclusion.class);
        this.initTemplates = buffer.readObjectArray(ServiceTemplate.class);
        this.initDeployments = buffer.readObjectArray(ServiceDeployment.class);

        this.deletedFilesAfterStop = buffer.readStringCollection().toArray(new String[0]);
        this.processConfig = buffer.readObject(ProcessConfiguration.class);
        this.port = buffer.readInt();

        super.properties = JsonDocument.newDocument(buffer.readString());
    }
}