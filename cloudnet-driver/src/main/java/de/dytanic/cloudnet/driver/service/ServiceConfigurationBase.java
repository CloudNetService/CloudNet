package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import java.util.Collection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
abstract class ServiceConfigurationBase extends BasicJsonDocPropertyable {

  protected Collection<ServiceRemoteInclusion> includes;

  protected Collection<ServiceTemplate> templates;

  protected Collection<ServiceDeployment> deployments;

  public ServiceConfigurationBase(Collection<ServiceRemoteInclusion> includes,
    Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments) {
    this.includes = includes;
    this.templates = templates;
    this.deployments = deployments;
  }
}