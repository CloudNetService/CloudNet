package de.dytanic.cloudnet.driver.service;

import java.util.Collection;

public class ServiceTask extends ServiceConfigurationBase {

    private String name;

    private String runtime;

    private boolean maintenance, autoDeleteOnStop, staticServices;

    private Collection<String> associatedNodes;

    private Collection<String> groups;

    private ProcessConfiguration processConfiguration;

    private int startPort, minServiceCount;

    public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
                       String name, String runtime, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes, Collection<String> groups,
                       ProcessConfiguration processConfiguration, int startPort, int minServiceCount) {
        this(includes, templates, deployments, name, runtime, false, autoDeleteOnStop,
                staticServices, associatedNodes, groups, processConfiguration, startPort, minServiceCount);
    }

    public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
                       String name, String runtime, boolean maintenance, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes, Collection<String> groups,
                       ProcessConfiguration processConfiguration, int startPort, int minServiceCount) {
        super(includes, templates, deployments);

        this.name = name;
        this.runtime = runtime;
        this.maintenance = maintenance;
        this.autoDeleteOnStop = autoDeleteOnStop;
        this.associatedNodes = associatedNodes;
        this.groups = groups;
        this.processConfiguration = processConfiguration;
        this.startPort = startPort;
        this.minServiceCount = minServiceCount;
        this.staticServices = staticServices;
    }

    public ServiceTask() {
    }

    public String getName() {
        return this.name;
    }

    public String getRuntime() {
        return this.runtime;
    }

    public boolean isMaintenance() {
        return this.maintenance;
    }

    public boolean isAutoDeleteOnStop() {
        return this.autoDeleteOnStop;
    }

    public boolean isStaticServices() {
        return this.staticServices;
    }

    public Collection<String> getAssociatedNodes() {
        return this.associatedNodes;
    }

    public Collection<String> getGroups() {
        return this.groups;
    }

    public ProcessConfiguration getProcessConfiguration() {
        return this.processConfiguration;
    }

    public int getStartPort() {
        return this.startPort;
    }

    public int getMinServiceCount() {
        return this.minServiceCount;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    public void setAutoDeleteOnStop(boolean autoDeleteOnStop) {
        this.autoDeleteOnStop = autoDeleteOnStop;
    }

    public void setStaticServices(boolean staticServices) {
        this.staticServices = staticServices;
    }

    public void setAssociatedNodes(Collection<String> associatedNodes) {
        this.associatedNodes = associatedNodes;
    }

    public void setGroups(Collection<String> groups) {
        this.groups = groups;
    }

    public void setProcessConfiguration(ProcessConfiguration processConfiguration) {
        this.processConfiguration = processConfiguration;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public void setMinServiceCount(int minServiceCount) {
        this.minServiceCount = minServiceCount;
    }

    public String toString() {
        return "ServiceTask(name=" + this.getName() + ", runtime=" + this.getRuntime() + ", maintenance=" + this.isMaintenance() + ", autoDeleteOnStop=" + this.isAutoDeleteOnStop() + ", staticServices=" + this.isStaticServices() + ", associatedNodes=" + this.getAssociatedNodes() + ", groups=" + this.getGroups() + ", processConfiguration=" + this.getProcessConfiguration() + ", startPort=" + this.getStartPort() + ", minServiceCount=" + this.getMinServiceCount() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ServiceTask)) return false;
        final ServiceTask other = (ServiceTask) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$runtime = this.getRuntime();
        final Object other$runtime = other.getRuntime();
        if (this$runtime == null ? other$runtime != null : !this$runtime.equals(other$runtime)) return false;
        if (this.isMaintenance() != other.isMaintenance()) return false;
        if (this.isAutoDeleteOnStop() != other.isAutoDeleteOnStop()) return false;
        if (this.isStaticServices() != other.isStaticServices()) return false;
        final Object this$associatedNodes = this.getAssociatedNodes();
        final Object other$associatedNodes = other.getAssociatedNodes();
        if (this$associatedNodes == null ? other$associatedNodes != null : !this$associatedNodes.equals(other$associatedNodes))
            return false;
        final Object this$groups = this.getGroups();
        final Object other$groups = other.getGroups();
        if (this$groups == null ? other$groups != null : !this$groups.equals(other$groups)) return false;
        final Object this$processConfiguration = this.getProcessConfiguration();
        final Object other$processConfiguration = other.getProcessConfiguration();
        if (this$processConfiguration == null ? other$processConfiguration != null : !this$processConfiguration.equals(other$processConfiguration))
            return false;
        if (this.getStartPort() != other.getStartPort()) return false;
        if (this.getMinServiceCount() != other.getMinServiceCount()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ServiceTask;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $runtime = this.getRuntime();
        result = result * PRIME + ($runtime == null ? 43 : $runtime.hashCode());
        result = result * PRIME + (this.isMaintenance() ? 79 : 97);
        result = result * PRIME + (this.isAutoDeleteOnStop() ? 79 : 97);
        result = result * PRIME + (this.isStaticServices() ? 79 : 97);
        final Object $associatedNodes = this.getAssociatedNodes();
        result = result * PRIME + ($associatedNodes == null ? 43 : $associatedNodes.hashCode());
        final Object $groups = this.getGroups();
        result = result * PRIME + ($groups == null ? 43 : $groups.hashCode());
        final Object $processConfiguration = this.getProcessConfiguration();
        result = result * PRIME + ($processConfiguration == null ? 43 : $processConfiguration.hashCode());
        result = result * PRIME + this.getStartPort();
        result = result * PRIME + this.getMinServiceCount();
        return result;
    }
}