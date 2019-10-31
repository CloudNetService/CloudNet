package de.dytanic.cloudnet.template.install;

import de.dytanic.cloudnet.driver.service.ServiceEnvironment;

import java.util.Collection;
import java.util.Optional;

public class ServiceVersionType {
    private String name;
    private ServiceEnvironment targetEnvironment;
    private InstallerType installerType;
    private Collection<ServiceVersion> versions;

    public ServiceVersionType(String name, ServiceEnvironment targetEnvironment, InstallerType installerType, Collection<ServiceVersion> versions) {
        this.name = name;
        this.targetEnvironment = targetEnvironment;
        this.installerType = installerType;
        this.versions = versions;
    }

    public Optional<ServiceVersion> getVersion(String name) {
        return this.versions.stream()
                .filter(serviceVersion -> serviceVersion.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public String getName() {
        return name;
    }

    public ServiceEnvironment getTargetEnvironment() {
        return targetEnvironment;
    }

    public InstallerType getInstallerType() {
        return installerType;
    }

    public Collection<ServiceVersion> getVersions() {
        return versions;
    }

    public enum InstallerType {
        DOWNLOAD,
        BUILD
    }

}
