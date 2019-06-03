package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.lang.reflect.Type;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CloudOfflinePlayer extends BasicJsonDocPropertyable implements
    ICloudOfflinePlayer {

  public static final Type TYPE = new TypeToken<CloudOfflinePlayer>() {
  }.getType();

  protected UUID uniqueId;

  protected String name, xBoxId;

  protected long firstLoginTimeMillis, lastLoginTimeMillis;

  protected NetworkConnectionInfo lastNetworkConnectionInfo;

  /*= --------------------------------------------------------------- =*/

  public static CloudOfflinePlayer of(ICloudPlayer cloudPlayer) {
    Validate.checkNotNull(cloudPlayer);

    CloudOfflinePlayer cloudOfflinePlayer = new CloudOfflinePlayer(
        cloudPlayer.getUniqueId(),
        cloudPlayer.getName(),
        cloudPlayer.getXBoxId(),
        cloudPlayer.getFirstLoginTimeMillis(),
        cloudPlayer.getLastLoginTimeMillis(),
        cloudPlayer.getLastNetworkConnectionInfo()
    );

    cloudOfflinePlayer.setProperties(cloudPlayer.getProperties());

    return cloudOfflinePlayer;
  }

  @Override
  public void setProperties(JsonDocument properties) {
    this.properties = properties;
  }
}