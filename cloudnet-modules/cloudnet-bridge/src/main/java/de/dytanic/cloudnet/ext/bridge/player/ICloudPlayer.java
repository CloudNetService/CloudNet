package de.dytanic.cloudnet.ext.bridge.player;

public interface ICloudPlayer extends ICloudOfflinePlayer {

    NetworkServiceInfo getLoginService();

    void setLoginService(NetworkServiceInfo loginService);

    NetworkServiceInfo getConnectedService();

    void setConnectedService(NetworkServiceInfo connectedService);

    NetworkConnectionInfo getNetworkConnectionInfo();

    void setNetworkConnectionInfo(NetworkConnectionInfo networkConnectionInfo);

    NetworkPlayerServerInfo getNetworkPlayerServerInfo();

    void setNetworkPlayerServerInfo(NetworkPlayerServerInfo networkPlayerServerInfo);

}