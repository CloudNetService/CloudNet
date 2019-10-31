package de.dytanic.cloudnet.template.install;

public class InvalidServiceVersionException extends RuntimeException {

    private ServiceVersion version;

    public InvalidServiceVersionException(ServiceVersion version, String message) {
        super("Version " + version.getName() + " has an invalid parameter: " + message);
        this.version = version;
    }

    public ServiceVersion getVersion() {
        return version;
    }
}
