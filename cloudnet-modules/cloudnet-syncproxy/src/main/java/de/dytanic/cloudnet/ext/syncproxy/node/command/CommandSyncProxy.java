package de.dytanic.cloudnet.ext.syncproxy.node.command;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfigurationWriterAndReader;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyMotd;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyTabList;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyTabListConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule;
import java.util.Collection;
import java.util.function.Predicate;

public final class CommandSyncProxy extends Command {

  public CommandSyncProxy() {
    super("syncproxy", "sp");

    this.permission = "cloudnet.console.command.syncproxy";
    this.prefix = "cloudnet-syncproxy";
    this.description = LanguageManager
        .getMessage("module-syncproxy-command-syncproxy-description");
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args,
      String commandLine, Properties properties) {
    if (args.length == 0) {
      sender.sendMessage(
          "syncproxy reload",
          "syncproxy list",
          "syncproxy target <group> maxPlayers <number>",
          "syncproxy target <group> maintenance <true : false>",
          "syncproxy target <group> whitelist",
          "syncproxy target <group> whitelist add <name>",
          "syncproxy target <group> whitelist remove <name>"
      );

      return;
    }

    if (args[0].equalsIgnoreCase("list")) {
      this.displayListConfiguration(sender,
          CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration());
      return;
    }

    if (args[0].equalsIgnoreCase("target") && args.length > 1) {
      SyncProxyProxyLoginConfiguration syncProxyLoginConfiguration = getSyncProxyLoginConfiguration(
          args[1]);

      if (syncProxyLoginConfiguration != null) {
        if (args.length > 2) {
          if (args.length == 4) {
            if (args[2].equalsIgnoreCase("maxPlayers") && Validate
                .testStringParseToInt(args[3])) {
              syncProxyLoginConfiguration
                  .setMaxPlayers(Integer.parseInt(args[3]));
              this.saveAndUpdate(CloudNetSyncProxyModule.getInstance()
                  .getSyncProxyConfiguration());

              sender.sendMessage(
                  LanguageManager
                      .getMessage("module-syncproxy-command-set-maxplayers")
                      .replace("%group%", args[1])
                      .replace("%count%", args[3])
              );
            }

            if (args[2].equalsIgnoreCase("maintenance")) {
              syncProxyLoginConfiguration
                  .setMaintenance(args[3].equalsIgnoreCase("true"));
              this.saveAndUpdate(CloudNetSyncProxyModule.getInstance()
                  .getSyncProxyConfiguration());

              sender.sendMessage(
                  LanguageManager
                      .getMessage("module-syncproxy-command-set-maintenance")
                      .replace("%group%", args[1])
                      .replace("%maintenance%", args[3])
              );
            }
          }

          if (args[2].equalsIgnoreCase("whitelist")) {
            if (args.length == 3) {
              this.displayWhitelist(sender,
                  syncProxyLoginConfiguration.getWhitelist());
              return;
            }

            if (args.length == 5) {
              if (args[3].equalsIgnoreCase("add")) {
                syncProxyLoginConfiguration.getWhitelist().add(args[4]);
                this.saveAndUpdate(CloudNetSyncProxyModule.getInstance()
                    .getSyncProxyConfiguration());

                sender.sendMessage(
                    LanguageManager.getMessage(
                        "module-syncproxy-command-add-whitelist-entry")
                        .replace("%group%", args[1])
                        .replace("%name%", args[4])
                );
              }

              if (args[3].equalsIgnoreCase("remove")) {
                syncProxyLoginConfiguration.getWhitelist().remove(args[4]);
                this.saveAndUpdate(CloudNetSyncProxyModule.getInstance()
                    .getSyncProxyConfiguration());

                sender.sendMessage(
                    LanguageManager.getMessage(
                        "module-syncproxy-command-remove-whitelist-entry")
                        .replace("%group%", args[1])
                        .replace("%name%", args[4])
                );
              }
            }
          }
        } else {
          this.displayConfiguration(sender, syncProxyLoginConfiguration);
        }
      }
      return;
    }

    if (args[0].equalsIgnoreCase("reload")) {
      CloudNetSyncProxyModule.getInstance()
          .setSyncProxyConfiguration(SyncProxyConfigurationWriterAndReader.read(
              CloudNetSyncProxyModule.getInstance().getConfigurationFile()
          ));

      CloudNetDriver.getInstance().sendChannelMessage(
          SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
          SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
          new JsonDocument("syncProxyConfiguration",
              CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration())
      );

      sender.sendMessage(LanguageManager
          .getMessage("module-syncproxy-command-reload-success"));
    }
  }

  private void saveAndUpdate(SyncProxyConfiguration syncProxyConfiguration) {
    SyncProxyConfigurationWriterAndReader.write(syncProxyConfiguration,
        CloudNetSyncProxyModule.getInstance().getConfigurationFile());

    CloudNetSyncProxyModule.getInstance()
        .setSyncProxyConfiguration(syncProxyConfiguration);
    CloudNetDriver.getInstance().sendChannelMessage(
        SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
        SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
        new JsonDocument("syncProxyConfiguration",
            CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration())
    );
  }

  private SyncProxyProxyLoginConfiguration getSyncProxyLoginConfiguration(
      String group) {
    return Iterables.first(
        CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration()
            .getLoginConfigurations(),
        new Predicate<SyncProxyProxyLoginConfiguration>() {
          @Override
          public boolean test(
              SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration) {
            return syncProxyProxyLoginConfiguration.getTargetGroup()
                .equalsIgnoreCase(group);
          }
        });
  }

  private void displayListConfiguration(ICommandSender sender,
      SyncProxyConfiguration syncProxyConfiguration) {
    for (SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration : syncProxyConfiguration
        .getLoginConfigurations()) {
      displayConfiguration(sender, syncProxyProxyLoginConfiguration);
    }

    for (SyncProxyTabListConfiguration syncProxyTabListConfiguration : syncProxyConfiguration
        .getTabListConfigurations()) {
      sender.sendMessage(
          "* " + syncProxyTabListConfiguration.getTargetGroup(),
          "AnimationsPerSecond: " + syncProxyTabListConfiguration
              .getAnimationsPerSecond(),
          " ",
          "Entries: "
      );

      int index = 1;
      for (SyncProxyTabList tabList : syncProxyTabListConfiguration
          .getEntries()) {
        sender.sendMessage(
            "- " + index++,
            "Header: " + tabList.getHeader().replace("&", "#"),
            "Footer: " + tabList.getFooter().replace("&", "#")
        );
      }
    }
  }

  private void displayConfiguration(ICommandSender sender,
      SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration) {
    sender.sendMessage(
        "* " + syncProxyProxyLoginConfiguration.getTargetGroup(),
        "Maintenance: " + (syncProxyProxyLoginConfiguration.isMaintenance()
            ? "enabled" : "disabled"),
        "Max-Players: " + syncProxyProxyLoginConfiguration.getMaxPlayers()
    );

    this.displayWhitelist(sender,
        syncProxyProxyLoginConfiguration.getWhitelist());

    sender.sendMessage("Motds:");
    for (SyncProxyMotd syncProxyMotd : syncProxyProxyLoginConfiguration
        .getMotds()) {
      this.displayMotd(sender, syncProxyMotd);
    }

    for (SyncProxyMotd syncProxyMotd : syncProxyProxyLoginConfiguration
        .getMaintenanceMotds()) {
      this.displayMotd(sender, syncProxyMotd);
    }
  }

  private void displayMotd(ICommandSender sender, SyncProxyMotd syncProxyMotd) {
    sender.sendMessage(
        "- Motd",
        "AutoSlot: " + syncProxyMotd.isAutoSlot(),
        "AutoSlot-MaxPlayerDistance: " + syncProxyMotd
            .getAutoSlotMaxPlayersDistance(),
        "Protocol-Text: " + syncProxyMotd.getProtocolText(),
        "First Line: " + syncProxyMotd.getFirstLine().replace("&", "#"),
        "Second Line: " + syncProxyMotd.getSecondLine().replace("&", "#"),
        "PlayerInfo: "
    );

    if (syncProxyMotd.getPlayerInfo() != null) {
      for (String playerInfoItem : syncProxyMotd.getPlayerInfo()) {
        sender.sendMessage("- " + playerInfoItem);
      }
    }
  }

  private void displayWhitelist(ICommandSender sender,
      Collection<String> whitelistEntries) {
    sender.sendMessage("Whitelist:");

    for (String whitelistEntry : whitelistEntries) {
      sender.sendMessage("- " + whitelistEntry);
    }
  }
}