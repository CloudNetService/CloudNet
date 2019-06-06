package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;

import java.util.Collection;

public final class ServiceDeployment extends BasicJsonDocPropertyable {

    private final ServiceTemplate template;

    private final Collection<String> excludes;

    public ServiceDeployment(ServiceTemplate template, Collection<String> excludes) {
        this.template = template;
        this.excludes = excludes;
    }

    public ServiceTemplate getTemplate() {
        return this.template;
    }

    public Collection<String> getExcludes() {
        return this.excludes;
    }

    public String toString() {
        return "ServiceDeployment(template=" + this.getTemplate() + ", excludes=" + this.getExcludes() + ")";
    }
}