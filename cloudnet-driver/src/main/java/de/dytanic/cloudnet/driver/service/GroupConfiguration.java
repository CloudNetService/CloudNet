package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.collection.Iterables;

import java.util.Collection;

public class GroupConfiguration extends ServiceConfigurationBase implements INameable {

    protected String name;
    protected Collection<String> jvmOptions = Iterables.newArrayList();
    protected Collection<ServiceEnvironmentType> targetEnvironments = Iterables.newArrayList();

    public GroupConfiguration() {
    }

    public GroupConfiguration(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments, String name, Collection<String> jvmOptions, Collection<ServiceEnvironmentType> targetEnvironments) {
        super(includes, templates, deployments);
        this.name = name;
        this.jvmOptions = jvmOptions;
        this.targetEnvironments = targetEnvironments;
    }

    public GroupConfiguration(String name) {
        this.name = name;
    }

    public Collection<String> getJvmOptions() {
        return jvmOptions;
    }

    public Collection<ServiceEnvironmentType> getTargetEnvironments() {
        return targetEnvironments;
    }

    public String getName() {
        return this.name;
    }

}