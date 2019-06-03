package de.dytanic.cloudnet.ext.bridge.nukkit;

import cn.nukkit.Server;
import cn.nukkit.event.HandlerList;
import cn.nukkit.plugin.PluginBase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.nukkit.listener.NukkitCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.nukkit.listener.NukkitPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;

public final class NukkitCloudNetBridgePlugin extends PluginBase {

    @Override
    public synchronized void onEnable() {
        this.initListeners();

        BridgeHelper.updateServiceInfo();
    }

    @Override
    public synchronized void onDisable() {
        HandlerList.unregisterAll(this);
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    private void initListeners() {
        //NukkitAPI
        Server.getInstance().getPluginManager().registerEvents(new NukkitPlayerListener(), this);

        //CloudNet
        CloudNetDriver.getInstance().getEventManager().registerListener(new NukkitCloudNetListener());
        CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
    }
}