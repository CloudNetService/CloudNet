package de.dytanic.cloudnet.ext.bridge.node;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.ProxyFallback;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.node.command.CommandPlayers;
import de.dytanic.cloudnet.ext.bridge.node.command.CommandReloadBridge;
import de.dytanic.cloudnet.ext.bridge.node.http.V1BridgeConfigurationHttpHandler;
import de.dytanic.cloudnet.ext.bridge.node.listener.IncludePluginListener;
import de.dytanic.cloudnet.ext.bridge.node.listener.NetworkListenerRegisterListener;
import de.dytanic.cloudnet.ext.bridge.node.listener.NodeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.node.listener.PlayerManagerListener;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;

public final class CloudNetBridgeModule extends NodeCloudNetModule {

    @Getter
    private static CloudNetBridgeModule instance;

    @Getter
    @Setter
    private BridgeConfiguration bridgeConfiguration;

    public CloudNetBridgeModule() {
        instance = this;
    }

    @ModuleTask(order = 64, event = ModuleLifeCycle.STARTED)
    public void createConfiguration() {
        this.getModuleWrapper().getDataFolder().mkdirs();

        this.bridgeConfiguration = getConfig().get("config", BridgeConfiguration.TYPE, new BridgeConfiguration(
                "&7Cloud &8| &b",
                Iterables.newArrayList(),
                Iterables.newArrayList(),
                Collections.singletonList(
                        new ProxyFallbackConfiguration(
                                "Proxy",
                                "Lobby",
                                Collections.singletonList(new ProxyFallback(1, "Lobby", null))
                        )
                ),
                Maps.of(
                        new Pair<>("command-hub-success-connect", "&7You did successfully connect to %server%"),
                        new Pair<>("command-hub-already-in-hub", "&cYou are already connected"),
                        new Pair<>("command-hub-no-server-found", "&7Hub server cannot be found"),
                        new Pair<>("server-join-cancel-because-only-proxy", "&7You must connect from a original proxy server")
                )
        ));

        if (this.bridgeConfiguration.getExcludedOnlyProxyWalkableGroups() == null)
            this.bridgeConfiguration.setExcludedOnlyProxyWalkableGroups(Iterables.newArrayList());

        saveConfig();
    }

    public void writeConfiguration(BridgeConfiguration bridgeConfiguration) {
        getConfig().append("config", bridgeConfiguration);
        saveConfig();
    }

    @ModuleTask(order = 36, event = ModuleLifeCycle.STARTED)
    public void initNodePlayerManager() {
        new NodePlayerManager("cloudnet_cloud_players");

        registerListener(new PlayerManagerListener());
    }

    @ModuleTask(order = 35, event = ModuleLifeCycle.STARTED)
    public void registerHandlers() {
        getHttpServer().registerHandler("/api/v1/modules/bridge/config",
                new V1BridgeConfigurationHttpHandler("cloudnet.http.v1.modules.bridge.config"));
    }

    @ModuleTask(order = 16, event = ModuleLifeCycle.STARTED)
    public void registerCommands() {
        registerCommand(new CommandReloadBridge());
        registerCommand(new CommandPlayers());
    }

    @ModuleTask(order = 8, event = ModuleLifeCycle.STARTED)
    public void initListeners() {
        registerListeners(new NetworkListenerRegisterListener(), new IncludePluginListener(), new NodeCustomChannelMessageListener());
    }
}