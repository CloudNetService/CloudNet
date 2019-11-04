package de.dytanic.cloudnet.template.install.installer;

import de.dytanic.cloudnet.template.install.ServiceVersion;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public interface ServiceVersionInstaller {

    void install(ServiceVersion version, Path workingDirectory, Callable<OutputStream[]> targetStreamCallable) throws Exception;

}
