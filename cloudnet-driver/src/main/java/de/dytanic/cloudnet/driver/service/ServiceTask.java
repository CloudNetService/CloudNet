package de.dytanic.cloudnet.driver.service;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

@ToString
@EqualsAndHashCode(callSuper = false)
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

}