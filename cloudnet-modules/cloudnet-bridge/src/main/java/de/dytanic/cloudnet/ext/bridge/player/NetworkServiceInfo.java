package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkServiceInfo {

    protected ServiceEnvironmentType environment;

    protected UUID uniqueId;

    protected String serverName;

}