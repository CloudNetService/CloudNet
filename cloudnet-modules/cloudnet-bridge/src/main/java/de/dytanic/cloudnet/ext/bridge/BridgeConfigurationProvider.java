package de.dytanic.cloudnet.ext.bridge;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public final class BridgeConfigurationProvider {

  private static final Type TYPE = new TypeToken<BridgeConfiguration>() {
  }.getType();

  private static BridgeConfiguration loadedConfiguration;

  private BridgeConfigurationProvider() {
    throw new UnsupportedOperationException();
  }

  public static BridgeConfiguration update(
    BridgeConfiguration bridgeConfiguration) {
    Validate.checkNotNull(bridgeConfiguration);

    CloudNetDriver.getInstance().sendChannelMessage(
      BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
      "update_bridge_configuration",
      new JsonDocument("bridgeConfiguration", bridgeConfiguration)
    );
    loadedConfiguration = bridgeConfiguration;

    return bridgeConfiguration;
  }

  public static void setLocal(BridgeConfiguration bridgeConfiguration) {
    Validate.checkNotNull(bridgeConfiguration);

    loadedConfiguration = bridgeConfiguration;
  }

  public static BridgeConfiguration load() {
    if (loadedConfiguration == null) {
      loadedConfiguration = load0();
    }

    return loadedConfiguration;
  }

  private static BridgeConfiguration load0() {
    ITask<BridgeConfiguration> task = CloudNetDriver.getInstance()
      .sendCallablePacket(
        CloudNetDriver.getInstance().getNetworkClient().getChannels()
          .iterator().next(),
        BridgeConstants.BRIDGE_NETWORK_CHANNEL_MESSAGE_GET_BRIDGE_CONFIGURATION_CHANNEL_NAME,
        BridgeConstants.BRIDGE_NETWORK_CHANNEL_MESSAGE_GET_BRIDGE_CONFIGURATION,
        new JsonDocument(),
        new Function<JsonDocument, BridgeConfiguration>() {
          @Override
          public BridgeConfiguration apply(JsonDocument documentPair) {
            return documentPair.get("bridgeConfig", TYPE);
          }
        });

    try {
      return task.get(5, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      e.printStackTrace();
    }

    return null;
  }
}