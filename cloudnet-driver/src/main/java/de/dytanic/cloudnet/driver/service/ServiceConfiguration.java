package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;

public final class ServiceConfiguration extends BasicJsonDocPropertyable {

    private final ServiceId serviceId;

    private final String runtime;

    private final boolean autoDeleteOnStop, staticService;

    private final String[] groups;

    private final ServiceRemoteInclusion[] includes;

    private final ServiceTemplate[] templates;

    private final ServiceDeployment[] deployments;

    private final ProcessConfiguration processConfig;

    private int port;

    public ServiceConfiguration(ServiceId serviceId, String runtime, boolean autoDeleteOnStop, boolean staticService, String[] groups, ServiceRemoteInclusion[] includes, ServiceTemplate[] templates, ServiceDeployment[] deployments, ProcessConfiguration processConfig, int port) {
        this.serviceId = serviceId;
        this.runtime = runtime;
        this.autoDeleteOnStop = autoDeleteOnStop;
        this.staticService = staticService;
        this.groups = groups;
        this.includes = includes;
        this.templates = templates;
        this.deployments = deployments;
        this.processConfig = processConfig;
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

    public ProcessConfiguration getProcessConfig() {
        return this.processConfig;
    }

    public int getPort() {
        return this.port;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ServiceConfiguration)) return false;
        final ServiceConfiguration other = (ServiceConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$serviceId = this.getServiceId();
        final Object other$serviceId = other.getServiceId();
        if (this$serviceId == null ? other$serviceId != null : !this$serviceId.equals(other$serviceId)) return false;
        final Object this$runtime = this.getRuntime();
        final Object other$runtime = other.getRuntime();
        if (this$runtime == null ? other$runtime != null : !this$runtime.equals(other$runtime)) return false;
        if (this.isAutoDeleteOnStop() != other.isAutoDeleteOnStop()) return false;
        if (this.isStaticService() != other.isStaticService()) return false;
        if (!java.util.Arrays.deepEquals(this.getGroups(), other.getGroups())) return false;
        if (!java.util.Arrays.deepEquals(this.getIncludes(), other.getIncludes())) return false;
        if (!java.util.Arrays.deepEquals(this.getTemplates(), other.getTemplates())) return false;
        if (!java.util.Arrays.deepEquals(this.getDeployments(), other.getDeployments())) return false;
        final Object this$processConfig = this.getProcessConfig();
        final Object other$processConfig = other.getProcessConfig();
        if (this$processConfig == null ? other$processConfig != null : !this$processConfig.equals(other$processConfig))
            return false;
        if (this.getPort() != other.getPort()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ServiceConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $serviceId = this.getServiceId();
        result = result * PRIME + ($serviceId == null ? 43 : $serviceId.hashCode());
        final Object $runtime = this.getRuntime();
        result = result * PRIME + ($runtime == null ? 43 : $runtime.hashCode());
        result = result * PRIME + (this.isAutoDeleteOnStop() ? 79 : 97);
        result = result * PRIME + (this.isStaticService() ? 79 : 97);
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getGroups());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getIncludes());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getTemplates());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getDeployments());
        final Object $processConfig = this.getProcessConfig();
        result = result * PRIME + ($processConfig == null ? 43 : $processConfig.hashCode());
        result = result * PRIME + this.getPort();
        return result;
    }

    public String toString() {
        return "ServiceConfiguration(serviceId=" + this.getServiceId() + ", runtime=" + this.getRuntime() + ", autoDeleteOnStop=" + this.isAutoDeleteOnStop() + ", staticService=" + this.isStaticService() + ", groups=" + java.util.Arrays.deepToString(this.getGroups()) + ", includes=" + java.util.Arrays.deepToString(this.getIncludes()) + ", templates=" + java.util.Arrays.deepToString(this.getTemplates()) + ", deployments=" + java.util.Arrays.deepToString(this.getDeployments()) + ", processConfig=" + this.getProcessConfig() + ", port=" + this.getPort() + ")";
    }

    public void setPort(int port) {
        this.port = port;
    }
}