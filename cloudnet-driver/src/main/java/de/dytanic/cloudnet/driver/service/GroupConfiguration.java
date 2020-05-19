package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class GroupConfiguration extends ServiceConfigurationBase implements INameable {

    protected String name;
    protected Collection<String> jvmOptions = new ArrayList<>();
    protected Collection<ServiceEnvironmentType> targetEnvironments = new ArrayList<>();

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
        return this.jvmOptions;
    }

    public Collection<ServiceEnvironmentType> getTargetEnvironments() {
        return this.targetEnvironments;
    }

    public String getName() {
        return this.name;
    }

}