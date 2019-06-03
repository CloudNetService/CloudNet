package de.dytanic.cloudnet.driver.service;

import java.util.Collection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ServiceTask extends ServiceConfigurationBase {

  private String name;

  private String runtime;

  private boolean maintenance, autoDeleteOnStop, staticServices;

  private Collection<String> associatedNodes;

  private Collection<String> groups;

  private ProcessConfiguration processConfiguration;

  private int startPort, minServiceCount;

  public ServiceTask(Collection<ServiceRemoteInclusion> includes,
      Collection<ServiceTemplate> templates,
      Collection<ServiceDeployment> deployments,
      String name, String runtime, boolean autoDeleteOnStop,
      boolean staticServices, Collection<String> associatedNodes,
      Collection<String> groups,
      ProcessConfiguration processConfiguration, int startPort,
      int minServiceCount) {
    this(includes, templates, deployments, name, runtime, false,
        autoDeleteOnStop,
        staticServices, associatedNodes, groups, processConfiguration,
        startPort, minServiceCount);
  }

  public ServiceTask(Collection<ServiceRemoteInclusion> includes,
      Collection<ServiceTemplate> templates,
      Collection<ServiceDeployment> deployments,
      String name, String runtime, boolean maintenance,
      boolean autoDeleteOnStop, boolean staticServices,
      Collection<String> associatedNodes, Collection<String> groups,
      ProcessConfiguration processConfiguration, int startPort,
      int minServiceCount) {
    super(includes, templates, deployments);

    this.name = name;
    this.runtime = runtime;
    this.maintenance = maintenance;
    this.autoDeleteOnStop = autoDeleteOnStop;
    this.associatedNodes = associatedNodes;
    this.groups = groups;
    this.processConfiguration = processConfiguration;
    this.startPort = startPort;
    this.minServiceCount = minServiceCount;
    this.staticServices = staticServices;
  }
}