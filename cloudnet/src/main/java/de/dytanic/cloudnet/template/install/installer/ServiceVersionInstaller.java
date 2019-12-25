package de.dytanic.cloudnet.template.install.installer;

import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersion;

import java.nio.file.Path;

public interface ServiceVersionInstaller {

    void install(ServiceVersion version, String fileName, Path workingDirectory, ITemplateStorage storage, ServiceTemplate targetTemplate, Path cachePath) throws Exception;

}
