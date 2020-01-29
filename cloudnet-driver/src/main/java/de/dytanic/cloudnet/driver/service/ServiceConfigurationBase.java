package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

@ToString
@EqualsAndHashCode(callSuper = false)
public abstract class ServiceConfigurationBase extends BasicJsonDocPropertyable {

    protected Collection<ServiceRemoteInclusion> includes;

    protected Collection<ServiceTemplate> templates;

    protected Collection<ServiceDeployment> deployments;

    public ServiceConfigurationBase(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments) {
        this.includes = includes;
        this.templates = templates;
        this.deployments = deployments;
    }

    public ServiceConfigurationBase() {
    }

    public abstract Collection<String> getJvmOptions();

    public Collection<ServiceRemoteInclusion> getIncludes() {
        return this.includes;
    }

    public void setIncludes(Collection<ServiceRemoteInclusion> includes) {
        this.includes = includes;
    }

    public Collection<ServiceTemplate> getTemplates() {
        return this.templates;
    }

    public void setTemplates(Collection<ServiceTemplate> templates) {
        this.templates = templates;
    }

    public Collection<ServiceDeployment> getDeployments() {
        return this.deployments;
    }

    public void setDeployments(Collection<ServiceDeployment> deployments) {
        this.deployments = deployments;
    }

}