package de.dytanic.cloudnet.ext.bridge.node.command;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.*;

public final class CommandPlayers extends SubCommandHandler {

    private static final long MAX_REGISTERED_PLAYERS_FOR_COMPLETION = 50;
    private static final long MAX_ONLINE_PLAYERS_FOR_COMPLETION = 50;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public CommandPlayers() {
        super(
                SubCommandBuilder.create()

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    sender.sendMessage("=> Online: " + NodePlayerManager.getInstance().getOnlineCount());
                                    if (NodePlayerManager.getInstance().getRegisteredCount() > MAX_ONLINE_PLAYERS_FOR_COMPLETION) {
                                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-too-many-players"));
                                        return;
                                    }

                                    for (ICloudPlayer cloudPlayer : NodePlayerManager.getInstance().getOnlinePlayers()) {
                                        sender.sendMessage("- " + cloudPlayer.getUniqueId() + " " + cloudPlayer.getName() + " | " +
                                                (cloudPlayer.getLoginService() != null ?
                                                        cloudPlayer.getLoginService().getUniqueId().toString().split("-")[0] + " " + cloudPlayer.getLoginService().getServerName() : null) +
                                                " | " +
                                                (cloudPlayer.getConnectedService() != null ?
                                                        cloudPlayer.getConnectedService().getUniqueId().toString().split("-")[0] + " " + cloudPlayer.getConnectedService().getServerName() : null)
                                        );
                                    }
                                },
                                anyStringIgnoreCase("online", "list")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    sender.sendMessage("=> Registered: " + NodePlayerManager.getInstance().getRegisteredCount());
                                    if (NodePlayerManager.getInstance().getRegisteredCount() > MAX_REGISTERED_PLAYERS_FOR_COMPLETION) {
                                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-too-many-players"));
                                        return;
                                    }

                                    for (ICloudOfflinePlayer cloudPlayer : NodePlayerManager.getInstance().getRegisteredPlayers()) {
                                        sender.sendMessage("- " + cloudPlayer.getUniqueId() + " " + cloudPlayer.getName() + " | Last login: " + DATE_FORMAT.format(new Date(cloudPlayer.getLastLoginTimeMillis())));
                                    }
                                },
                                anyStringIgnoreCase("registered", "all")
                        )

                        .prefix(anyStringIgnoreCase("player", "pl"))

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    String name = (String) args.argument("name").get();
                                    Collection<? extends ICloudOfflinePlayer> players = NodePlayerManager.getInstance().getOnlinePlayers(name);

                                    if (players.isEmpty()) {
                                        players = NodePlayerManager.getInstance().getOfflinePlayers(name);
                                    }

                                    for (ICloudOfflinePlayer player : players) {
                                        displayPlayer(sender, player);
                                    }
                                },
                                dynamicString(
                                        "name",
                                        LanguageManager.getMessage("module-bridge-command-players-player-not-registered"),
                                        name -> !NodePlayerManager.getInstance().getOfflinePlayers(name).isEmpty(),
                                        () -> NodePlayerManager.getInstance().getRegisteredCount() <= MAX_REGISTERED_PLAYERS_FOR_COMPLETION ?
                                                NodePlayerManager.getInstance().getRegisteredPlayers().stream()
                                                        .map(ICloudOfflinePlayer::getName)
                                                        .collect(Collectors.toList()) :
                                                null
                                )
                        )

                        .prefix(dynamicString(
                                "name",
                                LanguageManager.getMessage("module-bridge-command-players-player-not-online"),
                                name -> !NodePlayerManager.getInstance().getOnlinePlayers(name).isEmpty(),
                                () -> NodePlayerManager.getInstance().getRegisteredCount() <= MAX_ONLINE_PLAYERS_FOR_COMPLETION ?
                                        NodePlayerManager.getInstance().getOnlinePlayers().stream()
                                                .map(ICloudPlayer::getName)
                                                .collect(Collectors.toList()) : null
                        ))

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    String reason = (String) args.argument("reason").orElse("no reason given");
                                    for (ICloudPlayer player : NodePlayerManager.getInstance().getOnlinePlayers((String) args.argument("name").get())) {
                                        NodePlayerManager.getInstance().proxyKickPlayer(player, reason);
                                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-kick-player")
                                                .replace("%name%", player.getName())
                                                .replace("%uniqueId%", player.getUniqueId().toString())
                                                .replace("%reason%", reason)
                                        );
                                    }
                                },
                                subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length - 1).setMaxArgs(Integer.MAX_VALUE),
                                exactStringIgnoreCase("kick"),
                                dynamicString("reason")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    for (ICloudPlayer player : NodePlayerManager.getInstance().getOnlinePlayers((String) args.argument("name").get())) {
                                        NodePlayerManager.getInstance().proxySendPlayerMessage(player, (String) args.argument("message").orElseThrow(() -> new IllegalArgumentException("No message given")));
                                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-send-player-message")
                                                .replace("%name%", player.getName())
                                                .replace("%uniqueId%", player.getUniqueId().toString())
                                        );
                                    }
                                },
                                subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length - 1).setMaxArgs(Integer.MAX_VALUE),
                                exactStringIgnoreCase("sendMessage"),
                                dynamicString("message")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    String server = (String) args.argument("server").get();
                                    ServiceInfoSnapshot serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceProvider().getCloudServiceByName(server);
                                    if (!serviceInfoSnapshot.getConfiguration().getProcessConfig().getEnvironment().isMinecraftServer()) {
                                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-send-player-server-no-server"));
                                        return;
                                    }
                                    for (ICloudPlayer player : NodePlayerManager.getInstance().getOnlinePlayers((String) args.argument("name").get())) {
                                        NodePlayerManager.getInstance().proxySendPlayer(player, server);
                                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-send-player-server")
                                                .replace("%name%", player.getName())
                                                .replace("%uniqueId%", player.getUniqueId().toString())
                                                .replace("%server%", server)
                                        );
                                    }
                                },
                                exactStringIgnoreCase("connect"),
                                dynamicString(
                                        "server",
                                        LanguageManager.getMessage("module-bridge-command-players-send-player-server-not-found"),
                                        name -> CloudNet.getInstance().getCloudServiceProvider().getCloudServiceByName(name) != null,
                                        () -> CloudNet.getInstance().getCloudServiceProvider().getCloudServices().stream()
                                                .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getConfiguration().getProcessConfig().getEnvironment().isMinecraftServer())
                                                .map(ServiceInfoSnapshot::getName)
                                                .collect(Collectors.toList())
                                )
                        )

                        .getSubCommands(),
                "players", "player", "pl"
        );

        this.permission = "cloudnet.command.players";
        this.prefix = "cloudnet-bridge";
        this.description = LanguageManager.getMessage("module-bridge-command-players-description");
    }

    private static void displayPlayer(ICommandSender sender, ICloudOfflinePlayer cloudOfflinePlayer) {
        if (cloudOfflinePlayer == null) {
            return;
        }

        List<String> messages = new ArrayList<>();

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