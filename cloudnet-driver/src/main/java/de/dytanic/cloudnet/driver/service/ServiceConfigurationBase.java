package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;

import java.util.Collection;

abstract class ServiceConfigurationBase extends BasicJsonDocPropertyable {

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

    public Collection<ServiceRemoteInclusion> getIncludes() {
        return this.includes;
    }

    public Collection<ServiceTemplate> getTemplates() {
        return this.templates;
    }

    public Collection<ServiceDeployment> getDeployments() {
        return this.deployments;
    }

    public void setIncludes(Collection<ServiceRemoteInclusion> includes) {
        this.includes = includes;
    }

    public void setTemplates(Collection<ServiceTemplate> templates) {
        this.templates = templates;
    }

    public void setDeployments(Collection<ServiceDeployment> deployments) {
        this.deployments = deployments;
    }

    public String toString() {
        return "ServiceConfigurationBase(includes=" + this.getIncludes() + ", templates=" + this.getTemplates() + ", deployments=" + this.getDeployments() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ServiceConfigurationBase)) return false;
        final ServiceConfigurationBase other = (ServiceConfigurationBase) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$includes = this.getIncludes();
        final Object other$includes = other.getIncludes();
        if (this$includes == null ? other$includes != null : !this$includes.equals(other$includes)) return false;
        final Object this$templates = this.getTemplates();
        final Object other$templates = other.getTemplates();
        if (this$templates == null ? other$templates != null : !this$templates.equals(other$templates)) return false;
        final Object this$deployments = this.getDeployments();
        final Object other$deployments = other.getDeployments();
        if (this$deployments == null ? other$deployments != null : !this$deployments.equals(other$deployments))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ServiceConfigurationBase;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $includes = this.getIncludes();
        result = result * PRIME + ($includes == null ? 43 : $includes.hashCode());
        final Object $templates = this.getTemplates();
        result = result * PRIME + ($templates == null ? 43 : $templates.hashCode());
        final Object $deployments = this.getDeployments();
        result = result * PRIME + ($deployments == null ? 43 : $deployments.hashCode());
        return result;
    }
}