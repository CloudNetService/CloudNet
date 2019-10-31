package de.dytanic.cloudnet.template.install;

import java.util.Collection;

public class ServiceVersionType {

    private String name;
    private String fileName;
    private String installer;
    private Collection<ServiceVersion> versions;

    public ServiceVersionType(String name, String fileName, String installer, Collection<ServiceVersion> versions) {
        this.name = name;
        this.fileName = fileName;
        this.installer = installer;
        this.versions = versions;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public String getInstaller() {
        return installer;
    }

    public Collection<ServiceVersion> getVersions() {
        return versions;
    }
}
