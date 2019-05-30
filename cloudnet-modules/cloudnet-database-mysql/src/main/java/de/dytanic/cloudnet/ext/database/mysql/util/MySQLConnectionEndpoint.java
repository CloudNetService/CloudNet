package de.dytanic.cloudnet.ext.database.mysql.util;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class MySQLConnectionEndpoint {

  protected final boolean useSsl;

  protected final String database;

  protected final HostAndPort address;

}