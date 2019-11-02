package de.dytanic.cloudnet.ext.simplenametags;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.simplenametags.listener.CloudNetSimpleNameTagsListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class CloudNetSimpleNameTagsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Listener listener = new CloudNetSimpleNameTagsListener(this);

        getServer().getPluginManager().registerEvents(listener, this);
        CloudNetDriver.getInstance().getEventManager().registerListener(listener);
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }
}