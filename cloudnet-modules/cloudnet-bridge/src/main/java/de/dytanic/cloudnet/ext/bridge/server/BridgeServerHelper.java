package de.dytanic.cloudnet.ext.bridge.server;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;

public class BridgeServerHelper {

    private static volatile String motd;

    private static volatile String extra;

    private static volatile String state;

    private static volatile int maxPlayers;

    /**
     * @deprecated use {@link BridgeServerHelper#getMotd()} instead
     */
    @Deprecated
    public static String getApiMotd() {
        return BridgeServerHelper.getMotd();
    }

    /**
     * @deprecated use {@link BridgeServerHelper#setMotd(String)} instead
     */
    @Deprecated
    public static void setApiMotd(String apiMotd) {
        BridgeServerHelper.setMotd(apiMotd);
    }

    public static String getMotd() {
        return motd;
    }

    public static void setMotd(String motd) {
        BridgeServerHelper.motd = motd;
    }

    public static String getExtra() {
        return extra;
    }

    public static void setExtra(String extra) {
        BridgeServerHelper.extra = extra;
    }

    public static String getState() {
        return state;
    }

    public static void setState(String state) {
        BridgeServerHelper.state = state;
    }

    public static int getMaxPlayers() {
        return maxPlayers;
    }

    public static void setMaxPlayers(int maxPlayers) {
        BridgeServerHelper.maxPlayers = maxPlayers;
    }

    public static void updateServiceInfo() {
        BridgeHelper.updateServiceInfo();
    }

    public static void changeToIngame() {
        changeToIngame(true);
    }

    public static void changeToIngame(boolean autoStartService) {
        BridgeServerHelper.state = "INGAME";
        BridgeHelper.updateServiceInfo();

        if (!autoStartService) {
            return;
        }

        String task = Wrapper.getInstance().getServiceId().getTaskName();

        CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTaskAsync(task).onComplete(serviceTask -> {
            if (serviceTask != null) {
                CloudNetDriver.getInstance().getCloudServiceFactory().createCloudServiceAsync(serviceTask).onComplete(serviceInfoSnapshot -> {
                    if (serviceInfoSnapshot != null) {
                        serviceInfoSnapshot.provider().start();
                    }
                });
            }
        });
    }

}
