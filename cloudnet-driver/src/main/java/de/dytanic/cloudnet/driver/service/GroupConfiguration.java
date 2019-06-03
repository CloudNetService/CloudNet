package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;

@Getter
@AllArgsConstructor
public class GroupConfiguration extends ServiceConfigurationBase implements INameable {

    protected String name;

    public GroupConfiguration(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments, String name) {
        super(includes, templates, deployments);
        this.name = name;
    }
}