package de.dytanic.cloudnet.ext.bridge.node;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.ProxyFallback;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.node.command.CommandPlayers;
import de.dytanic.cloudnet.ext.bridge.node.command.CommandReloadBridge;
import de.dytanic.cloudnet.ext.bridge.node.http.V1BridgeConfigurationHttpHandler;
import de.dytanic.cloudnet.ext.bridge.node.listener.*;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class CloudNetBridgeModule extends NodeCloudNetModule {

    private static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>();

    static {
        DEFAULT_MESSAGES.put("command-hub-success-connect", "&7You did successfully connect to %server%");
        DEFAULT_MESSAGES.put("command-hub-already-in-hub", "&cYou are already connected");
        DEFAULT_MESSAGES.put("command-hub-no-server-found", "&7Hub server cannot be found");
        DEFAULT_MESSAGES.put("server-join-cancel-because-only-proxy", "&7You must connect from a original proxy server");
        DEFAULT_MESSAGES.put("server-join-cancel-because-maintenance", "&7This server is currently in maintenance mode");
        DEFAULT_MESSAGES.put("command-cloud-sub-command-no-permission", "&7You are not allowed to use &b%command%");
    }

    private static CloudNetBridgeModule instance;

    private BridgeConfiguration bridgeConfiguration;

    private final NodePlayerManager nodePlayerManager = new NodePlayerManager("cloudnet_cloud_players");

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
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
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

    public ProxyFallbackConfiguration createDefaultFallbackConfiguration(String targetGroup) {
        return new ProxyFallbackConfiguration(
                targetGroup,
                "Lobby",
                Collections.singletonList(new ProxyFallback("Lobby", null, 1))
        );
    }

    public void writeConfiguration(BridgeConfiguration bridgeConfiguration) {
        getConfig().append("config", bridgeConfiguration);
        saveConfig();
    }

    @ModuleTask(order = 36, event = ModuleLifeCycle.STARTED)
    public void initNodePlayerManager() {
        super.getCloudNet().getServicesRegistry().registerService(IPlayerManager.class, "NodePlayerManager", this.nodePlayerManager);

        registerListener(new PlayerManagerListener(this.nodePlayerManager));
    }

    @ModuleTask(order = 35, event = ModuleLifeCycle.STARTED)
    public void registerHandlers() {
        getHttpServer().registerHandler("/api/v1/modules/bridge/config",
                new V1BridgeConfigurationHttpHandler("cloudnet.http.v1.modules.bridge.config"));
    }

    @ModuleTask(order = 16, event = ModuleLifeCycle.STARTED)
    public void registerCommands() {
        registerCommand(new CommandReloadBridge());
        registerCommand(new CommandPlayers(this.nodePlayerManager));
    }

    @ModuleTask(order = 8, event = ModuleLifeCycle.STARTED)
    public void initListeners() {
        registerListeners(new NetworkListenerRegisterListener(), new BridgeTaskSetupListener(), new IncludePluginListener(),
                new NodeCustomChannelMessageListener(this.nodePlayerManager), new BridgeDefaultConfigurationListener(), new BridgeServiceListCommandListener());
    }

    @Override
    public JsonDocument reloadConfig() {
        getModuleWrapper().getDataFolder().mkdirs();
        File file = new File(getModuleWrapper().getDataFolder(), "config.json");

        if (!file.exists()) {
            this.createConfiguration();
        }

        return super.config = JsonDocument.newDocument(file);
    }

    public BridgeConfiguration getBridgeConfiguration() {
        return this.bridgeConfiguration;
    }

    public void setBridgeConfiguration(BridgeConfiguration bridgeConfiguration) {
        this.bridgeConfiguration = bridgeConfiguration;
    }
}