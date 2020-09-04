package de.dytanic.cloudnet.ext.bridge.node;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.ProxyFallback;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.listener.TaskConfigListener;
import de.dytanic.cloudnet.ext.bridge.node.command.CommandBridge;
import de.dytanic.cloudnet.ext.bridge.node.command.CommandPlayers;
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
        DEFAULT_MESSAGES.put("server-join-cancel-because-only-proxy", "&7You must connect from an original proxy server");
        DEFAULT_MESSAGES.put("server-join-cancel-because-maintenance", "&7This server is currently in maintenance mode");
        DEFAULT_MESSAGES.put("server-join-cancel-because-permission", "&7You do not have the required permissions to connect to this server.");
        DEFAULT_MESSAGES.put("command-cloud-sub-command-no-permission", "&7You are not allowed to use &b%command%");
        DEFAULT_MESSAGES.put("already-connected", "§cYou are already connected to this network!");
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

        this.bridgeConfiguration = this.getConfig().get("config", BridgeConfiguration.TYPE, new BridgeConfiguration(
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

        this.getConfig().append("config", this.bridgeConfiguration);
        this.saveConfig();
    }

    public ProxyFallbackConfiguration createDefaultFallbackConfiguration(String targetGroup) {
        return new ProxyFallbackConfiguration(
                targetGroup,
                "Lobby",
                Collections.singletonList(new ProxyFallback("Lobby", null, 1))
        );
    }

    public void writeConfiguration(BridgeConfiguration bridgeConfiguration) {
        this.getConfig().append("config", bridgeConfiguration);
        this.saveConfig();
    }

    @ModuleTask(order = 36, event = ModuleLifeCycle.STARTED)
    public void initNodePlayerManager() {
        super.getCloudNet().getServicesRegistry().registerService(IPlayerManager.class, "NodePlayerManager", this.nodePlayerManager);

        this.registerListener(new PlayerManagerListener(this.nodePlayerManager));
    }

    @ModuleTask(order = 35, event = ModuleLifeCycle.STARTED)
    public void registerHandlers() {
        this.getHttpServer().registerHandler("/api/v1/modules/bridge/config",
                new V1BridgeConfigurationHttpHandler("cloudnet.http.v1.modules.bridge.config"));
    }

    @ModuleTask(order = 17, event = ModuleLifeCycle.STARTED)
    public void checkTaskConfigurations() {
        // adding a required join permission option to all minecraft-server-based tasks, if not existing
        this.getCloudNet().getServiceTaskProvider().getPermanentServiceTasks().forEach(serviceTask -> {
            if (serviceTask.getProcessConfiguration().getEnvironment().isMinecraftServer() && !serviceTask.getProperties().contains("requiredPermission")) {
                serviceTask.getProperties().appendNull("requiredPermission");
                this.getCloudNet().getServiceTaskProvider().addPermanentServiceTask(serviceTask);
            }
        });
    }

    @ModuleTask(order = 16, event = ModuleLifeCycle.STARTED)
    public void registerCommands() {
        this.registerCommand(new CommandBridge());
        this.registerCommand(new CommandPlayers(this.nodePlayerManager));
    }

    @ModuleTask(order = 8, event = ModuleLifeCycle.STARTED)
    public void initListeners() {
        this.registerListeners(new NetworkListenerRegisterListener(), new BridgeTaskSetupListener(), new IncludePluginListener(),
                new NodeCustomChannelMessageListener(this.nodePlayerManager), new BridgePlayerDisconnectListener(this.nodePlayerManager),
                new BridgeDefaultConfigurationListener(), new BridgeServiceListCommandListener(), new TaskConfigListener());
    }

    @Override
    public JsonDocument reloadConfig() {
        this.getModuleWrapper().getDataFolder().mkdirs();
        File file = new File(this.getModuleWrapper().getDataFolder(), "config.json");

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