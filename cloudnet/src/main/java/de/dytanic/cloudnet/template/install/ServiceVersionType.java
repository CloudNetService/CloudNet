package de.dytanic.cloudnet.template.install;

import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.template.install.run.step.InstallStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ServiceVersionType {

  private String name;
  private ServiceEnvironment targetEnvironment;
  private List<InstallStep> installSteps = new ArrayList<>();
  private Collection<ServiceVersion> versions;

  public ServiceVersionType() {
  }

  public ServiceVersionType(String name, ServiceEnvironment targetEnvironment, List<InstallStep> installSteps,
    Collection<ServiceVersion> versions) {
    this.name = name;
    this.targetEnvironment = targetEnvironment;
    this.installSteps = installSteps;
    this.versions = versions;
  }

  public Optional<ServiceVersion> getVersion(String name) {
    return this.versions.stream()
      .filter(serviceVersion -> serviceVersion.getName().equalsIgnoreCase(name))
      .findFirst();
  }

  public boolean canInstall(ServiceVersion serviceVersion) {
    return !this.installSteps.contains(InstallStep.BUILD) || serviceVersion.canRun();
  }

  public String getName() {
    return this.name;
  }

  public ServiceEnvironment getTargetEnvironment() {
    return this.targetEnvironment;
  }

  public List<InstallStep> getInstallSteps() {
    return this.installSteps;
  }

  public Collection<ServiceVersion> getVersions() {
    return this.versions;
  }

}
