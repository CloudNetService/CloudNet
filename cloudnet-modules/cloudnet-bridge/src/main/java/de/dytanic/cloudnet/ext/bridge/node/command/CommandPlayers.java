package de.dytanic.cloudnet.ext.bridge.node.command;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public final class CommandPlayers extends Command {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public CommandPlayers() {
        super("players", "player", "pl");

        this.permission = "cloudnet.console.command.players";
        this.prefix = "cloudnet-bridge";
        this.usage = "players online";
        this.description = LanguageManager.getMessage("module-bridge-command-players-description");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    "players online",
                    "players player <name>",
                    "players player <name> kick <reason>",
                    "players player <name> send <target>",
                    "players player <name> message <message>"
            );
            return;
        }

        if (args[0].equalsIgnoreCase("online")) {
            for (ICloudPlayer cloudPlayer : NodePlayerManager.getInstance().getOnlinePlayers())
                sender.sendMessage("- " + cloudPlayer.getUniqueId() + " " + cloudPlayer.getName() + " | " +
                        (cloudPlayer.getLoginService() != null ?
                                cloudPlayer.getLoginService().getUniqueId().toString().split("-")[0] + " " + cloudPlayer.getLoginService().getServerName() : null) +
                        " | " +
                        (cloudPlayer.getConnectedService() != null ?
                                cloudPlayer.getConnectedService().getUniqueId().toString().split("-")[0] + " " + cloudPlayer.getConnectedService().getServerName() : null)
                );
            return;
        }

        if (args[0].equalsIgnoreCase("player") && args.length > 1) {
            List<? extends ICloudOfflinePlayer> cloudPlayers = NodePlayerManager.getInstance().getOnlinePlayer(args[1]);

            if (cloudPlayers.isEmpty())
                cloudPlayers = NodePlayerManager.getInstance().getOfflinePlayer(args[1]);

            if (args.length < 4) {
                for (ICloudOfflinePlayer cloudOfflinePlayer : cloudPlayers)
                    displayPlayer(sender, cloudOfflinePlayer);

                return;
            }

            if (cloudPlayers.isEmpty()) return;

            ICloudOfflinePlayer cloudOfflinePlayer = cloudPlayers.get(0);

            if (cloudOfflinePlayer instanceof ICloudPlayer) {
                ICloudPlayer cloudPlayer = (ICloudPlayer) cloudOfflinePlayer;

                switch (args[2].toLowerCase()) {
                    case "kick":
                        BridgePlayerManager.getInstance().proxyKickPlayer(cloudPlayer, buildMessage(args, 3));
                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-send-player-server"));
                        break;
                    case "message":
                        BridgePlayerManager.getInstance().proxySendPlayerMessage(cloudPlayer, buildMessage(args, 3));
                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-send-player-message"));
                        break;
                    case "send":
                        BridgePlayerManager.getInstance().proxySendPlayer(cloudPlayer, args[3]);
                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-kick-player"));
                        break;
                }
            }
        }
    }

    private String buildMessage(String[] args, int startIndex) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = startIndex; i < args.length; ++i)
            stringBuilder.append(args[i]).append(" ");

        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    private void displayPlayer(ICommandSender sender, ICloudOfflinePlayer cloudOfflinePlayer) {
        if (cloudOfflinePlayer == null) return;

        List<String> messages = Iterables.newArrayList();

        messages.addAll(Arrays.asList(
                "* CloudPlayer " + cloudOfflinePlayer.getUniqueId() + " " + cloudOfflinePlayer.getName(),
                "- XBoxId: " + cloudOfflinePlayer.getXBoxId(),
                "- First Login: " + DATE_FORMAT.format(cloudOfflinePlayer.getFirstLoginTimeMillis()),
                "- Last Login: " + DATE_FORMAT.format(cloudOfflinePlayer.getLastLoginTimeMillis())
        ));

        messages.addAll(Arrays.asList(
                "- Last NetworkConnectionInfo: ",
                "  Player Address: " + cloudOfflinePlayer.getLastNetworkConnectionInfo().getAddress(),
                "  Listener: " + cloudOfflinePlayer.getLastNetworkConnectionInfo().getListener(),
                "  Version: " + cloudOfflinePlayer.getLastNetworkConnectionInfo().getVersion(),
                "  Online mode: " + cloudOfflinePlayer.getLastNetworkConnectionInfo().isOnlineMode(),
                "  Legacy: " + cloudOfflinePlayer.getLastNetworkConnectionInfo().isLegacy()
        ));

        if (cloudOfflinePlayer instanceof ICloudPlayer) {
            ICloudPlayer cloudPlayer = (ICloudPlayer) cloudOfflinePlayer;

            messages.addAll(Arrays.asList(
                    "- Current NetworkConnectionInfo: ",
                    "  Player Address: " + cloudPlayer.getNetworkConnectionInfo().getAddress(),
                    "  Listener: " + cloudPlayer.getNetworkConnectionInfo().getListener(),
                    "  Version: " + cloudPlayer.getNetworkConnectionInfo().getVersion(),
                    "  Online mode: " + cloudPlayer.getNetworkConnectionInfo().isOnlineMode(),
                    "  Legacy: " + cloudPlayer.getNetworkConnectionInfo().isLegacy()
            ));

            messages.add("- Login Service: " + (cloudPlayer.getLoginService() != null ?
                    cloudPlayer.getLoginService().getUniqueId() + " " + cloudPlayer.getLoginService().getServerName()
                    :
                    null
            ));

            messages.add("- Connected Service: " + (cloudPlayer.getConnectedService() != null ?
                    cloudPlayer.getConnectedService().getUniqueId() + " " + cloudPlayer.getConnectedService().getServerName()
                    :
                    null
            ));
        }

        messages.add("- Properties: ");
        messages.addAll(Arrays.asList(cloudOfflinePlayer.getProperties().toPrettyJson().split("\n")));
        messages.add(" ");

        sender.sendMessage(messages.toArray(new String[0]));
    }
}