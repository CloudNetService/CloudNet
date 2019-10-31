package de.dytanic.cloudnet.template.install;

import java.io.OutputStream;
import java.nio.file.Path;

public interface ServiceVersionInstaller {

    void install(ServiceVersion version, Path workingDirectory, OutputStream targetStream) throws Exception;

}
