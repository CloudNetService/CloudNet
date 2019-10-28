package de.dytanic.cloudnet.driver.service;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceTask extends ServiceConfigurationBase {

    private String name;

    private String runtime;

    private boolean maintenance, autoDeleteOnStop, staticServices;

    private Collection<String> associatedNodes = new ArrayList<>();

    private Collection<String> groups = new ArrayList<>();

    private Collection<String> deletedFilesAfterStop = new ArrayList<>();

    private ProcessConfiguration processConfiguration;

    private int startPort;
    private int minServiceCount = 0;

    /**
     * Represents the time in millis where this task is able to start new services again
     */
    private transient long serviceStartAbilityTime = -1;

    public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
                       String name, String runtime, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes, Collection<String> groups,
                       ProcessConfiguration processConfiguration, int startPort, int minServiceCount) {
        this(includes, templates, deployments, name, runtime, false, autoDeleteOnStop,
                staticServices, associatedNodes, groups, processConfiguration, startPort, minServiceCount);
    }

    public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
                       String name, String runtime, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes, Collection<String> groups,
                       Collection<String> deletedFilesAfterStop, ProcessConfiguration processConfiguration, int startPort, int minServiceCount) {
        this(includes, templates, deployments, name, runtime, false, autoDeleteOnStop, staticServices, associatedNodes, groups, deletedFilesAfterStop, processConfiguration, startPort, minServiceCount);
    }

    public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
                       String name, String runtime, boolean maintenance, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes, Collection<String> groups,
                       ProcessConfiguration processConfiguration, int startPort, int minServiceCount) {
        this(includes, templates, deployments, name, runtime, maintenance, autoDeleteOnStop, staticServices, associatedNodes, groups, new ArrayList<>(), processConfiguration, startPort, minServiceCount);
    }

    public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
                       String name, String runtime, boolean maintenance, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes, Collection<String> groups,
                       Collection<String> deletedFilesAfterStop, ProcessConfiguration processConfiguration, int startPort, int minServiceCount) {
        super(includes, templates, deployments);
        this.name = name;
        this.runtime = runtime;
        this.maintenance = maintenance;
        this.autoDeleteOnStop = autoDeleteOnStop;
        this.associatedNodes = associatedNodes;
        this.groups = groups;
        this.deletedFilesAfterStop = deletedFilesAfterStop;
        this.processConfiguration = processConfiguration;
        this.startPort = startPort;
        this.minServiceCount = minServiceCount;
        this.staticServices = staticServices;
    }

    public ServiceTask() {
    }

    /**
     * Forbids this task to start new services for a specific time
     *
     * @param time the time in millis
     */
    public void forbidServiceStarting(long time) {
        this.serviceStartAbilityTime = System.currentTimeMillis() + time;
    }

    public boolean canStartServices() {
        return !this.maintenance && System.currentTimeMillis() > this.serviceStartAbilityTime;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuntime() {
        return this.runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public boolean isMaintenance() {
        return this.maintenance;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    public boolean isAutoDeleteOnStop() {
        return this.autoDeleteOnStop;
    }

    public void setAutoDeleteOnStop(boolean autoDeleteOnStop) {
        this.autoDeleteOnStop = autoDeleteOnStop;
    }

    public boolean isStaticServices() {
        return this.staticServices;
    }

    public void setStaticServices(boolean staticServices) {
        this.staticServices = staticServices;
    }

    public Collection<String> getAssociatedNodes() {
        return this.associatedNodes;
    }

    public void setAssociatedNodes(Collection<String> associatedNodes) {
        this.associatedNodes = associatedNodes;
    }

    public Collection<String> getGroups() {
        return this.groups;
    }

    public void setGroups(Collection<String> groups) {
        this.groups = groups;
    }

    public Collection<String> getDeletedFilesAfterStop() {
        return deletedFilesAfterStop;
    }

    public void setDeletedFilesAfterStop(Collection<String> deletedFilesAfterStop) {
        this.deletedFilesAfterStop = deletedFilesAfterStop;
    }

    public ProcessConfiguration getProcessConfiguration() {
        return this.processConfiguration;
    }

    public void setProcessConfiguration(ProcessConfiguration processConfiguration) {
        this.processConfiguration = processConfiguration;
    }

    public int getStartPort() {
        return this.startPort;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public int getMinServiceCount() {
        return this.minServiceCount;
    }

    public void setMinServiceCount(int minServiceCount) {
        this.minServiceCount = minServiceCount;
    }

}