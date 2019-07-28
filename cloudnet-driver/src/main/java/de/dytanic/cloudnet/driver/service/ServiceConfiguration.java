package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class ServiceConfiguration extends BasicJsonDocPropertyable {

    private final ServiceId serviceId;

    private final String runtime;

    private final boolean autoDeleteOnStop, staticService;

    private final String[] groups;

    private final ServiceRemoteInclusion[] includes;

    private final ServiceTemplate[] templates;

    private final ServiceDeployment[] deployments;

    private final String[] deletedFilesAfterStop;

    private final ProcessConfiguration processConfig;

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

    public String getRuntime() {
        return this.runtime;
    }

    public boolean isAutoDeleteOnStop() {
        return this.autoDeleteOnStop;
    }

    public boolean isStaticService() {
        return this.staticService;
    }

    public String[] getGroups() {
        return this.groups;
    }

    public ServiceRemoteInclusion[] getIncludes() {
        return this.includes;
    }

    public ServiceTemplate[] getTemplates() {
        return this.templates;
    }

    public ServiceDeployment[] getDeployments() {
        return this.deployments;
    }

    public String[] getDeletedFilesAfterStop() {
        return this.deletedFilesAfterStop;
    }

    public ProcessConfiguration getProcessConfig() {
        return this.processConfig;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}