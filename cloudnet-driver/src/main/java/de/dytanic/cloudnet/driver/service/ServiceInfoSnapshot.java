package de.dytanic.cloudnet.driver.service;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import java.lang.reflect.Type;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ServiceInfoSnapshot extends BasicJsonDocPropertyable {

  public static final Type TYPE = new TypeToken<ServiceInfoSnapshot>() {
  }.getType();

  protected long creationTime;

  protected ServiceId serviceId;

  protected HostAndPort address;

  @Setter
  protected boolean connected;

  @Setter
  protected ServiceLifeCycle lifeCycle;

  @Setter
  protected ProcessSnapshot processSnapshot;

  protected ServiceConfiguration configuration;

}