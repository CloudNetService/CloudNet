package de.dytanic.cloudnet.ext.syncproxy;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncProxyConfiguration {

  public static final Type TYPE = new TypeToken<SyncProxyConfiguration>() {
  }.getType();

  protected Collection<SyncProxyProxyLoginConfiguration> loginConfigurations;

  protected Collection<SyncProxyTabListConfiguration> tabListConfigurations;

  protected Map<String, String> messages;

}