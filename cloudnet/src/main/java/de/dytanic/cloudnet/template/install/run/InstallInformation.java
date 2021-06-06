package de.dytanic.cloudnet.template.install.run;

import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;

public class InstallInformation {

  private final ServiceVersionType serviceVersionType;
  private final ServiceVersion serviceVersion;
  private final TemplateStorage templateStorage;
  private final ServiceTemplate serviceTemplate;

  public InstallInformation(ServiceVersionType serviceVersionType, ServiceVersion serviceVersion,
    TemplateStorage templateStorage, ServiceTemplate serviceTemplate) {
    this.serviceVersionType = serviceVersionType;
    this.serviceVersion = serviceVersion;
    this.templateStorage = templateStorage;
    this.serviceTemplate = serviceTemplate;
  }

  public ServiceVersionType getServiceVersionType() {
    return serviceVersionType;
  }

  public ServiceVersion getServiceVersion() {
    return serviceVersion;
  }

  public TemplateStorage getTemplateStorage() {
    return templateStorage;
  }

  public ServiceTemplate getServiceTemplate() {
    return serviceTemplate;
  }

}
