package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class ServiceConfiguration extends BasicJsonDocPropertyable {

  private final ServiceId serviceId;

  private final String runtime;

  private final boolean autoDeleteOnStop, staticService;

  private final String[] groups;

  private final ServiceRemoteInclusion[] includes;

  private final ServiceTemplate[] templates;

  private final ServiceDeployment[] deployments;

  private final ProcessConfiguration processConfig;

  @Setter
  private int port;

}