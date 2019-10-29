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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class CloudNetBridgeModule extends NodeCloudNetModule {

    private static final Map<String, String> DEFAULT_MESSAGES = Maps.of(
            new Pair<>("command-hub-success-connect", "&7You did successfully connect to %server%"),
            new Pair<>("command-hub-already-in-hub", "&cYou are already connected"),
            new Pair<>("command-hub-no-server-found", "&7Hub server cannot be found"),
            new Pair<>("server-join-cancel-because-only-proxy", "&7You must connect from a original proxy server"),
            new Pair<>("server-join-cancel-because-maintenance", "&7This task is currently in maintenance mode"),
            new Pair<>("command-cloud-sub-command-no-permission", "&7You are not allowed to use &e%command%")
    );

    private static CloudNetBridgeModule instance;

    private BridgeConfiguration bridgeConfiguration;

    public CloudNetBridgeModule() {
        instance = this;
    }

    public static CloudNetBridgeModule getInstance() {
        return CloudNetBridgeModule.instance;
    }

    @ModuleTask(order = 64, event = ModuleLifeCycle.STARTED)
    public void createConfiguration() {
        this.getModuleWrapper().getDataFolder().mkdirs();

        this.bridgeConfiguration = getConfig().get("config", BridgeConfiguration.TYPE, new BridgeConfiguration(
                "&7Cloud &8| &b",
                true,
                Iterables.newArrayList(),
                Iterables.newArrayList(),
                Collections.singletonList(
                        new ProxyFallbackConfiguration(
                                "Proxy",
                                "Lobby",
                                Collections.singletonList(new ProxyFallback("Lobby", null, 1))
                        )
                ),
                DEFAULT_MESSAGES,
                true
        ));

        if (this.bridgeConfiguration.getMessages() != null) {
            for (Map.Entry<String, String> entry : DEFAULT_MESSAGES.entrySet()) {
                if (!this.bridgeConfiguration.getMessages().containsKey(entry.getKey())) {
                    this.bridgeConfiguration.getMessages().put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            this.bridgeConfiguration.setMessages(new HashMap<>(DEFAULT_MESSAGES));
        }

        getConfig().append("config", this.bridgeConfiguration);
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

    public BridgeConfiguration getBridgeConfiguration() {
        return this.bridgeConfiguration;
    }

    public void setBridgeConfiguration(BridgeConfiguration bridgeConfiguration) {
        this.bridgeConfiguration = bridgeConfiguration;
    }
}