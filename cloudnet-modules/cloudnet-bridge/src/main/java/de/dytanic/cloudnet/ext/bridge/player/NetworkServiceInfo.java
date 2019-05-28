package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkServiceInfo {

  protected ServiceEnvironmentType environment;

  protected UUID uniqueId;

  protected String serverName;

}