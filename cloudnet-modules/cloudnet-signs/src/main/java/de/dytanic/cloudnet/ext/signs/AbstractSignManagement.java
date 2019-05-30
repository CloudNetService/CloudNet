package de.dytanic.cloudnet.ext.signs;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;

public abstract class AbstractSignManagement {

  @Getter
  protected static AbstractSignManagement instance;

  @Getter
  @Setter
  protected List<Sign> signs;

  public abstract void onRegisterService(
    ServiceInfoSnapshot serviceInfoSnapshot);

  public abstract void onStartService(ServiceInfoSnapshot serviceInfoSnapshot);

  public abstract void onConnectService(
    ServiceInfoSnapshot serviceInfoSnapshot);

  public abstract void onUpdateServiceInfo(
    ServiceInfoSnapshot serviceInfoSnapshot);

  public abstract void onDisconnectService(
    ServiceInfoSnapshot serviceInfoSnapshot);

  public abstract void onStopService(ServiceInfoSnapshot serviceInfoSnapshot);

  public abstract void onUnregisterService(
    ServiceInfoSnapshot serviceInfoSnapshot);

  public abstract void onSignAdd(Sign sign);

  public abstract void onSignRemove(Sign sign);

  public void sendSignAddUpdate(Sign sign) {
    Validate.checkNotNull(sign);

    CloudNetDriver.getInstance()
      .sendChannelMessage(
        SignConstants.SIGN_CHANNEL_NAME,
        SignConstants.SIGN_CHANNEL_ADD_SIGN_MESSAGE,
        new JsonDocument("sign", sign)
      );
  }

  public void sendSignRemoveUpdate(Sign sign) {
    Validate.checkNotNull(sign);

    CloudNetDriver.getInstance()
      .sendChannelMessage(
        SignConstants.SIGN_CHANNEL_NAME,
        SignConstants.SIGN_CHANNEL_REMOVE_SIGN_MESSAGE,
        new JsonDocument("sign", sign)
      );
  }

  public Collection<Sign> getSignsFromNode() {
    ITask<Collection<Sign>> signs = CloudNetDriver.getInstance()
      .sendCallablePacket(
        CloudNetDriver.getInstance().getNetworkClient().getChannels()
          .iterator().next(),
        SignConstants.SIGN_CHANNEL_SYNC_CHANNEL_PROPERTY,
        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY,
          SignConstants.SIGN_CHANNEL_SYNC_ID_GET_SIGNS_COLLECTION_PROPERTY),
        new byte[0],
        new Function<Pair<JsonDocument, byte[]>, Collection<Sign>>() {
          @Override
          public Collection<Sign> apply(
            Pair<JsonDocument, byte[]> documentPair) {
            return documentPair.getFirst()
              .get("signs", SignConstants.COLLECTION_SIGNS);
          }
        }
      );

    try {
      return signs.get(5, TimeUnit.SECONDS);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return null;
  }

  public void updateSignConfiguration(SignConfiguration signConfiguration) {
    Validate.checkNotNull(signConfiguration);

    CloudNetDriver.getInstance().sendChannelMessage(
      SignConstants.SIGN_CHANNEL_NAME,
      SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION,
      new JsonDocument("signConfiguration", signConfiguration)
    );
  }

  public boolean isImportantCloudService(
    ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot != null &&
      (
        serviceInfoSnapshot.getServiceId().getEnvironment()
          == ServiceEnvironmentType.MINECRAFT_SERVER ||
          serviceInfoSnapshot.getServiceId().getEnvironment()
            == ServiceEnvironmentType.GLOWSTONE
      )
      ;
  }
}