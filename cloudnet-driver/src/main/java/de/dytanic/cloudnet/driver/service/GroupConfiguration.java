package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class GroupConfiguration extends ServiceConfigurationBase implements INameable {

    protected String name;

    public GroupConfiguration(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments, String name) {
        super(includes, templates, deployments);
        this.name = name;
    }

    public GroupConfiguration(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}