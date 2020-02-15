package de.dytanic.cloudnet.ext.syncproxy.node.command;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.syncproxy.*;
import de.dytanic.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule;

import java.util.Collection;
import java.util.stream.Collectors;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.*;

public final class CommandSyncProxy extends SubCommandHandler {

    public CommandSyncProxy() {
        super(
                SubCommandBuilder.create()

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    CloudNetSyncProxyModule.getInstance().setSyncProxyConfiguration(SyncProxyConfigurationWriterAndReader.read(
                                            CloudNetSyncProxyModule.getInstance().getConfigurationFile()
                                    ));

                                    CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                                            SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
                                            SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
                                            new JsonDocument("syncProxyConfiguration", CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration())
                                    );

                                    sender.sendMessage(LanguageManager.getMessage("module-syncproxy-command-reload-success"));
                                },
                                anyStringIgnoreCase("reload", "rl")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> displayListConfiguration(sender, CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration()),
                                exactStringIgnoreCase("list")
                        )

                        .prefix(exactStringIgnoreCase("target"))
                        .prefix(dynamicString(
                                "targetGroup",
                                LanguageManager.getMessage("module-signs-command-create-entry-group-not-found"),
                                name -> getSyncProxyLoginConfiguration(name) != null,
                                () -> CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration().getLoginConfigurations().stream().map(SyncProxyProxyLoginConfiguration::getTargetGroup).collect(Collectors.toList())
                        ))

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    String targetGroup = (String) args.argument("targetGroup").get();
                                    int value = (int) args.argument("value").get();
                                    SyncProxyProxyLoginConfiguration configuration = getSyncProxyLoginConfiguration(targetGroup);

                                    configuration.setMaxPlayers(value);

                                    saveAndUpdate(CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration());

                                    sender.sendMessage(
                                            LanguageManager.getMessage("module-syncproxy-command-set-maxplayers")
                                                    .replace("%group%", configuration.getTargetGroup())
                                                    .replace("%count%", String.valueOf(value))
                                    );
                                },
                                exactStringIgnoreCase("maxPlayers"),
                                positiveInteger("value")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    String targetGroup = (String) args.argument("targetGroup").get();
                                    boolean enabled = (boolean) args.argument("enabled").get();
                                    SyncProxyProxyLoginConfiguration configuration = getSyncProxyLoginConfiguration(targetGroup);

                                    configuration.setMaintenance(enabled);

                                    saveAndUpdate(CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration());

                                    sender.sendMessage(
                                            LanguageManager.getMessage("module-syncproxy-command-set-maintenance")
                                                    .replace("%group%", configuration.getTargetGroup())
                                                    .replace("%maintenance%", String.valueOf(enabled))
                                    );
                                },
                                exactStringIgnoreCase("maxPlayers"),
                                boolean_("enabled")
                        )

                        .prefix(exactStringIgnoreCase("whitelist"))

                        .generateCommand((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            String targetGroup = (String) args.argument("targetGroup").get();
                            SyncProxyProxyLoginConfiguration configuration = getSyncProxyLoginConfiguration(targetGroup);
                            displayWhitelist(sender, configuration.getWhitelist());
                        })

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    String targetGroup = (String) args.argument("targetGroup").get();
                                    String name = (String) args.argument("name").get();

                                    SyncProxyProxyLoginConfiguration configuration = getSyncProxyLoginConfiguration(targetGroup);
                                    configuration.getWhitelist().add(name);
                                    saveAndUpdate(CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration());

                                    sender.sendMessage(
                                            LanguageManager.getMessage("module-syncproxy-command-add-whitelist-entry")
                                                    .replace("%group%", configuration.getTargetGroup())
                                                    .replace("%name%", name)
                                    );
                                },
                                exactStringIgnoreCase("add"),
                                dynamicString("name")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    String targetGroup = (String) args.argument("targetGroup").get();
                                    String name = (String) args.argument("name").get();

                                    SyncProxyProxyLoginConfiguration configuration = getSyncProxyLoginConfiguration(targetGroup);
                                    configuration.getWhitelist().remove(name);
                                    saveAndUpdate(CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration());

                                    sender.sendMessage(
                                            LanguageManager.getMessage("module-syncproxy-command-remove-whitelist-entry")
                                                    .replace("%group%", configuration.getTargetGroup())
                                                    .replace("%name%", name)
                                    );
                                },
                                exactStringIgnoreCase("remove"),
                                dynamicString("name")
                        )

                        .getSubCommands(),
                "syncproxy", "sp"
        );

        this.permission = "cloudnet.command.syncproxy";
        this.prefix = "cloudnet-syncproxy";
        this.description = LanguageManager.getMessage("module-syncproxy-command-syncproxy-description");
    }

    private static void saveAndUpdate(SyncProxyConfiguration syncProxyConfiguration) {
        SyncProxyConfigurationWriterAndReader.write(syncProxyConfiguration, CloudNetSyncProxyModule.getInstance().getConfigurationFile());

        CloudNetSyncProxyModule.getInstance().setSyncProxyConfiguration(syncProxyConfiguration);
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
                SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
                new JsonDocument("syncProxyConfiguration", CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration())
        );
    }

    private static SyncProxyProxyLoginConfiguration getSyncProxyLoginConfiguration(String group) {
        return CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration().getLoginConfigurations().stream()
                .filter(syncProxyProxyLoginConfiguration -> syncProxyProxyLoginConfiguration.getTargetGroup().equalsIgnoreCase(group))
                .findFirst()
                .orElse(null);
    }

    private static void displayListConfiguration(ICommandSender sender, SyncProxyConfiguration syncProxyConfiguration) {
        for (SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration : syncProxyConfiguration.getLoginConfigurations()) {
            displayConfiguration(sender, syncProxyProxyLoginConfiguration);
        }

        for (SyncProxyTabListConfiguration syncProxyTabListConfiguration : syncProxyConfiguration.getTabListConfigurations()) {
            sender.sendMessage(
                    "* " + syncProxyTabListConfiguration.getTargetGroup(),
                    "AnimationsPerSecond: " + syncProxyTabListConfiguration.getAnimationsPerSecond(),
                    " ",
                    "Entries: "
            );

            int index = 1;
            for (SyncProxyTabList tabList : syncProxyTabListConfiguration.getEntries()) {
                sender.sendMessage(
                        "- " + index++,
                        "Header: " + tabList.getHeader(),
                        "Footer: " + tabList.getFooter()
                );
            }
        }
    }

    private static void displayConfiguration(ICommandSender sender, SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration) {
        sender.sendMessage(
                "* " + syncProxyProxyLoginConfiguration.getTargetGroup(),
                "Maintenance: " + (syncProxyProxyLoginConfiguration.isMaintenance() ? "enabled" : "disabled"),
                "Max-Players: " + syncProxyProxyLoginConfiguration.getMaxPlayers()
        );

        displayWhitelist(sender, syncProxyProxyLoginConfiguration.getWhitelist());

        sender.sendMessage("Motds:");
        for (SyncProxyMotd syncProxyMotd : syncProxyProxyLoginConfiguration.getMotds()) {
            displayMotd(sender, syncProxyMotd);
        }

        for (SyncProxyMotd syncProxyMotd : syncProxyProxyLoginConfiguration.getMaintenanceMotds()) {
            displayMotd(sender, syncProxyMotd);
        }
    }

    private static void displayMotd(ICommandSender sender, SyncProxyMotd syncProxyMotd) {
        sender.sendMessage(
                "- Motd",
                "AutoSlot: " + syncProxyMotd.isAutoSlot(),
                "AutoSlot-MaxPlayerDistance: " + syncProxyMotd.getAutoSlotMaxPlayersDistance(),
                "Protocol-Text: " + syncProxyMotd.getProtocolText(),
                "First Line: " + syncProxyMotd.getFirstLine(),
                "Second Line: " + syncProxyMotd.getSecondLine(),
                "PlayerInfo: "
        );

        if (syncProxyMotd.getPlayerInfo() != null) {
            for (String playerInfoItem : syncProxyMotd.getPlayerInfo()) {
                sender.sendMessage("- " + playerInfoItem);
            }
        }
    }

    private static void displayWhitelist(ICommandSender sender, Collection<String> whitelistEntries) {
        sender.sendMessage("Whitelist:");

        for (String whitelistEntry : whitelistEntries) {
            sender.sendMessage("- " + whitelistEntry);
        }
    }
}