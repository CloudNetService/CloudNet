package de.dytanic.cloudnet.ext.signs;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public final class SignConfigurationProvider {

  private static volatile SignConfiguration loadedConfiguration;

  private SignConfigurationProvider() {
    throw new UnsupportedOperationException();
  }

  public static void setLocal(SignConfiguration signConfiguration) {
    Validate.checkNotNull(signConfiguration);

    loadedConfiguration = signConfiguration;
  }

  public static SignConfiguration load() {
    if (loadedConfiguration == null) {
      loadedConfiguration = load0();
    }

    return loadedConfiguration;
  }

  private static SignConfiguration load0() {
    ITask<SignConfiguration> task = CloudNetDriver.getInstance()
      .sendCallablePacket(
        CloudNetDriver.getInstance().getNetworkClient().getChannels()
          .iterator().next(),
        SignConstants.SIGN_CHANNEL_SYNC_CHANNEL_PROPERTY,
        SignConstants.SIGN_CHANNEL_SYNC_ID_GET_SIGNS_CONFIGURATION_PROPERTY,
        new JsonDocument(),
        new Function<JsonDocument, SignConfiguration>() {
          @Override
          public SignConfiguration apply(JsonDocument documentPair) {
            return documentPair
              .get("signConfiguration", SignConfiguration.TYPE);
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