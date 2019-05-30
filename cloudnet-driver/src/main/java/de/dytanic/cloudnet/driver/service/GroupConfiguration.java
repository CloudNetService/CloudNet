package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupConfiguration extends ServiceConfigurationBase implements
  INameable {

  protected String name;

  public GroupConfiguration(Collection<ServiceRemoteInclusion> includes,
    Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments, String name) {
    super(includes, templates, deployments);
    this.name = name;
  }
}