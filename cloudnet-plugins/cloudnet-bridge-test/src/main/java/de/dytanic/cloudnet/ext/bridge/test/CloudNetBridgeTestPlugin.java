package de.dytanic.cloudnet.ext.bridge.test;

import org.bukkit.plugin.java.JavaPlugin;

public final class CloudNetBridgeTestPlugin extends JavaPlugin {

    @Override
    public void onEnable()
    {
        getCommand("cn-test").setPermission("cloudnet.test.command.permission");
        getCommand("cn-test").setExecutor(new CommandTest());
    }

    @Override
    public void onDisable()
    {

    }
}