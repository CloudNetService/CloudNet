package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;

public interface ICloudPlayer extends ICloudOfflinePlayer, SerializableObject {

  NetworkServiceInfo getLoginService();

  void setLoginService(NetworkServiceInfo loginService);

  NetworkServiceInfo getConnectedService();

  void setConnectedService(NetworkServiceInfo connectedService);

  NetworkConnectionInfo getNetworkConnectionInfo();

  void setNetworkConnectionInfo(NetworkConnectionInfo networkConnectionInfo);

  NetworkPlayerServerInfo getNetworkPlayerServerInfo();

  void setNetworkPlayerServerInfo(NetworkPlayerServerInfo networkPlayerServerInfo);

  JsonDocument getOnlineProperties();

  PlayerExecutor getPlayerExecutor();

}
