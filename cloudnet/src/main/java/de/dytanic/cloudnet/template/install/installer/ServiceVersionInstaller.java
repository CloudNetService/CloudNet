package de.dytanic.cloudnet.template.install.installer;

import de.dytanic.cloudnet.template.install.ServiceVersion;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface ServiceVersionInstaller {

    void install(ServiceVersion version, Path workingDirectory, OutputStream... targetStream) throws IOException;

}
