package de.dytanic.cloudnet;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.ConsoleColor;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;

import java.util.HashMap;
import java.util.Map;

public class DefaultInstallation {

    private CloudNet cloudNet;

    private final Map<String, Runnable> defaultTaskInstallations = new HashMap<>();

    {
        this.defaultTaskInstallations.put("nothing", () -> {});
        this.defaultTaskInstallations.put("recommended", this::installRecommended);
        this.defaultTaskInstallations.put("bedrock", this::installBedrock);
        this.defaultTaskInstallations.put("java-bungee-1.8.8", () -> this.installJavaBungee("default", "spigot-1.8.8"));
        this.defaultTaskInstallations.put("java-bungee-1.13.2", () -> this.installJavaBungee("default", "spigot-1.13.2"));
        this.defaultTaskInstallations.put("java-bungee-1.14.4", () -> this.installJavaBungee("default", "paperspigot-1.14.4"));
        this.defaultTaskInstallations.put("java-velocity-1.8.8", () -> this.installJavaVelocity("default", "spigot-1.8.8"));
        this.defaultTaskInstallations.put("java-velocity-1.13.2", () -> this.installJavaVelocity("default", "spigot-1.13.2"));
    }

    public DefaultInstallation(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    public void initDefaultConfigDefaultHostAddress() throws Exception {
        if (!this.cloudNet.getConfig().isFileExists()) {
            String input;

            do {
                if (System.getProperty("cloudnet.config.default-address") != null) {
                    this.cloudNet.getConfig().setDefaultHostAddress(System.getProperty("cloudnet.config.default-address"));
                    break;
                }

                if (System.getenv("CLOUDNET_CONFIG_IP_ADDRESS") != null) {
                    this.cloudNet.getConfig().setDefaultHostAddress(System.getenv("CLOUDNET_CONFIG_IP_ADDRESS"));
                    break;
                }

                this.cloudNet.getLogger().info(ConsoleColor.DARK_GRAY + LanguageManager.getMessage("cloudnet-init-config-hostaddress-input"));

                this.cloudNet.getConsole().resetPrompt();
                this.cloudNet.getConsole().setPrompt(ConsoleColor.WHITE.toString());
                input = this.cloudNet.getConsole().readLineNoPrompt();
                this.cloudNet.getConsole().setPrompt(ConsoleColor.DEFAULT.toString());
                this.cloudNet.getConsole().resetPrompt();

                if (!input.equals("127.0.1.1") && input.split("\\.").length == 4) {
                    this.cloudNet.getConfig().setDefaultHostAddress(input);
                    break;

                } else {
                    this.cloudNet.getLogger().warning(ConsoleColor.RED + LanguageManager.getMessage("cloudnet-init-config-hostaddress-input-invalid"));
                }

            } while (true);
        }
    }

    public void initDefaultPermissionGroups() {
        if (this.cloudNet.getPermissionManagement().getGroups().isEmpty() && System.getProperty("cloudnet.default.permissions.skip") == null) {
            IPermissionGroup adminPermissionGroup = new PermissionGroup("Admin", 100);
            adminPermissionGroup.addPermission("*");
            adminPermissionGroup.addPermission("Proxy", "*");
            adminPermissionGroup.setPrefix("&4Admin &8| &7");
            adminPermissionGroup.setColor("&7");
            adminPermissionGroup.setSuffix("&f");
            adminPermissionGroup.setDisplay("&4");
            adminPermissionGroup.setSortId(10);

            this.cloudNet.getPermissionManagement().addGroup(adminPermissionGroup);

            IPermissionGroup defaultPermissionGroup = new PermissionGroup("default", 100);
            defaultPermissionGroup.addPermission("bukkit.broadcast.user", true);
            defaultPermissionGroup.setDefaultGroup(true);
            defaultPermissionGroup.setPrefix("&7");
            defaultPermissionGroup.setColor("&7");
            defaultPermissionGroup.setSuffix("&f");
            defaultPermissionGroup.setDisplay("&7");
            defaultPermissionGroup.setSortId(10);

            this.cloudNet.getPermissionManagement().addGroup(defaultPermissionGroup);
        }
    }

    public void initDefaultTasks() throws Exception {
        if (this.cloudNet.getCloudServiceManager().getGroupConfigurations().isEmpty() && this.cloudNet.getCloudServiceManager().getServiceTasks().isEmpty() &&
                System.getProperty("cloudnet.default.tasks.skip") == null) {
            while (this.shouldTryAgain()) {

                String input = this.readTaskInput().trim().toLowerCase();
                if (this.defaultTaskInstallations.containsKey(input)) {
                    this.defaultTaskInstallations.get(input).run();
                    break;
                } else {
                    this.cloudNet.getLogger().warning(ConsoleColor.RED + LanguageManager.getMessage("cloudnet-init-default-tasks-input-invalid"));
                }
            }
        }
    }

    private void installRecommended() {
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create task Proxy bungeecord");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create task Lobby minecraft_server");

        //Create groups
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create group Global-Server");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create group Global-Proxy");

        //Add groups
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Proxy add group Global-Proxy");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Lobby add group Global-Server");

        //Install
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "lt create Global bukkit minecraft_server");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "lt install Global bukkit minecraft_server paperspigot-1.12.2");

        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "lt create Global proxy bungeecord");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "lt install Global proxy bungeecord default");

        //Add templates
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks group Global-Server add template local Global bukkit");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks group Global-Proxy add template local Global proxy");

        //Set configurations
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Proxy set minServiceCount 1");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Lobby set minServiceCount 1");
    }

    private void installBedrock() {
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create task Proxy waterdog");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create task Lobby nukkit");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "lt install Proxy default waterdog default");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "lt install Lobby default nukkit default");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Proxy set minServiceCount 1");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Lobby set minServiceCount 1");
    }

    private void installJavaBungee(String bungeeVersion, String spigotVersion) {
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create task Proxy bungeecord");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create task Lobby minecraft_server");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "lt install Proxy default bungeecord " + bungeeVersion);
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "lt install Lobby default minecraft_server " + spigotVersion);
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Proxy set minServiceCount 1");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Lobby set minServiceCount 1");
    }

    private void installJavaVelocity(String velocityVersion, String spigotVersion) {
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create task Proxy velocity");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create task Lobby minecraft_server");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "lt install Proxy default velocity " + velocityVersion);
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "lt install Lobby default minecraft_server " + spigotVersion);
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Proxy set minServiceCount 1");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Lobby set minServiceCount 1");
    }

    private String readTaskInput() throws Exception {
        String input;

        if (System.getProperty("cloudnet.default.tasks.installation") != null) {
            input = System.getProperty("cloudnet.default.tasks.installation");
        } else if (System.getenv("CLOUDNET_DEFAULT_TASKS_INSTALLATION") != null) {
            input = System.getenv("CLOUDNET_DEFAULT_TASKS_INSTALLATION");
        } else {
            this.cloudNet.getLogger().info(ConsoleColor.DARK_GRAY + LanguageManager.getMessage("cloudnet-init-default-tasks-input"));
            this.cloudNet.getLogger().info(ConsoleColor.DARK_GRAY + String.join(", ", this.defaultTaskInstallations.keySet()));

            this.cloudNet.getConsole().resetPrompt();
            this.cloudNet.getConsole().setPrompt(ConsoleColor.WHITE.toString());
            input = this.cloudNet.getConsole().readLineNoPrompt();
            this.cloudNet.getConsole().setPrompt(ConsoleColor.DEFAULT.toString());
            this.cloudNet.getConsole().resetPrompt();
        }

        return input;
    }

    public boolean shouldTryAgain() {
        return System.getProperty("cloudnet.default.tasks.installation") == null && System.getenv("CLOUDNET_DEFAULT_TASKS_INSTALLATION") == null;
    }
    
}
