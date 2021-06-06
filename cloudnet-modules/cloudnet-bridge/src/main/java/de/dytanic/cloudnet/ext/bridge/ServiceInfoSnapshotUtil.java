package de.dytanic.cloudnet.ext.bridge;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.property.ServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import java.util.Collection;

public final class ServiceInfoSnapshotUtil {

  private ServiceInfoSnapshotUtil() {
    throw new UnsupportedOperationException();
  }

  public static int getTaskOnlineCount(String taskName) {
    return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class)
      .taskOnlinePlayers(taskName)
      .count();
  }

  public static int getGroupOnlineCount(String groupName) {
    return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class)
      .groupOnlinePlayers(groupName)
      .count();
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link
   * BridgeServiceProperty#VERSION}.
   */
  @Deprecated
  public static String getVersion(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.VERSION).orElse(null);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link
   * BridgeServiceProperty#ONLINE_COUNT}.
   */
  @Deprecated
  public static int getOnlineCount(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link
   * BridgeServiceProperty#MAX_PLAYERS}.
   */
  @Deprecated
  public static int getMaxPlayers(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.MAX_PLAYERS).orElse(0);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link
   * BridgeServiceProperty#IS_ONLINE}.
   */
  @Deprecated
  public static boolean isOnline(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_ONLINE).orElse(false);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link
   * BridgeServiceProperty#IS_EMPTY}.
   */
  @Deprecated
  public static boolean isEmptyService(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_EMPTY).orElse(false);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link
   * BridgeServiceProperty#IS_FULL}.
   */
  @Deprecated
  public static boolean isFullService(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_FULL).orElse(false);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link
   * BridgeServiceProperty#IS_STARTING}.
   */
  @Deprecated
  public static boolean isStartingService(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_STARTING).orElse(false);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link
   * BridgeServiceProperty#IS_IN_GAME}.
   */
  @Deprecated
  public static boolean isIngameService(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link BridgeServiceProperty#MOTD}.
   */
  @Deprecated
  public static String getMotd(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.MOTD).orElse(null);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link BridgeServiceProperty#STATE}.
   */
  @Deprecated
  public static String getState(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.STATE).orElse(null);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link BridgeServiceProperty#EXTRA}.
   */
  @Deprecated
  public static String getExtra(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.EXTRA).orElse(null);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link
   * BridgeServiceProperty#PLUGINS}.
   */
  @Deprecated
  public static Collection<PluginInfo> getPlugins(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperty(BridgeServiceProperty.PLUGINS).orElse(null);
  }

  /**
   * @deprecated Use {@link ServiceInfoSnapshot#getProperty(ServiceProperty)} with {@link
   * BridgeServiceProperty#PLAYERS}.
   */
  @Deprecated
  public static Collection<JsonDocument> getPlayers(ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.getProperties().get("Players", new TypeToken<Collection<JsonDocument>>() {
    }.getType());
  }
}
