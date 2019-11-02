package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class ServiceConfiguration extends BasicJsonDocPropertyable {

    private ServiceId serviceId;

    private String runtime;

    private boolean autoDeleteOnStop, staticService;

    private String[] groups;

    private ServiceRemoteInclusion[] includes;

    private ServiceTemplate[] templates;

    private ServiceDeployment[] deployments;

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
        this.includes = includes;
        this.templates = templates;
        this.deployments = deployments;
        this.deletedFilesAfterStop = deletedFilesAfterStop;
        this.processConfig = processConfig;
        this.properties = properties;
        this.port = port;
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
}