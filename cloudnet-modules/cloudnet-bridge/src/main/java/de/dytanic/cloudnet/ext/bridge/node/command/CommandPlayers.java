package de.dytanic.cloudnet.ext.bridge.node.command;

import com.google.common.primitives.Longs;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.*;

public final class CommandPlayers extends SubCommandHandler {

    private static final long MAX_REGISTERED_PLAYERS_FOR_COMPLETION = 50;
    private static final long MAX_ONLINE_PLAYERS_FOR_COMPLETION = 50;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public CommandPlayers(IPlayerManager playerManager) {
        super(
                SubCommandBuilder.create()

                        .prefix(anyStringIgnoreCase("online", "list"))
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    Long limit = properties.containsKey("limit") ? Longs.tryParse(properties.get("limit")) : null;
                                    if (limit == null) {
                                        limit = Long.MAX_VALUE;
                                    }
                                    long currentIndex = 0;

                                    List<String> messages = new ArrayList<>();

                                    for (ICloudPlayer player : playerManager.onlinePlayers().asPlayers()) {
                                        if (properties.containsKey("name") &&
                                                !player.getName().toLowerCase().contains(properties.get("name").toLowerCase())) {
                                            continue;
                                        }

                                        if (currentIndex++ >= limit) {
                                            break;
                                        }

                                        if (properties.containsKey("connect")) {
                                            playerManager.getPlayerExecutor(player).connect(properties.get("connect"));
                                        }
                                        if (properties.containsKey("message")) {
                                            playerManager.getPlayerExecutor(player).sendChatMessage(properties.get("message"));
                                        }
                                        if (properties.containsKey("kick")) {
                                            playerManager.getPlayerExecutor(player).kick(properties.get("kick"));
                                        }
                                        if (properties.containsKey("showName")) {
                                            messages.add(player.getName());
                                        }
                                    }

                                    if (!messages.isEmpty()) {
                                        Collections.sort(messages);
                                        sender.sendMessage(String.join("; ", messages));
                                    }
                                },
                                subCommand -> subCommand.enableProperties().appendUsage("| limit=50 | connect=Lobby-1 | \"message=Message to a User\" | \"kick=You got kicked\" | --showName | name=derrop"),
                                anyStringIgnoreCase("foreach", "for")
                        )
                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    sender.sendMessage("=> Online: " + playerManager.getOnlineCount());
                                    if (!properties.containsKey("force") && playerManager.getOnlineCount() > MAX_ONLINE_PLAYERS_FOR_COMPLETION) {
                                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-too-many-players"));
                                        return;
                                    }

                                    for (ICloudPlayer cloudPlayer : playerManager.onlinePlayers().asPlayers()) {
                                        sender.sendMessage("- " + cloudPlayer.getUniqueId() + " " + cloudPlayer.getName() + " | " +
                                                (cloudPlayer.getLoginService() != null ?
                                                        cloudPlayer.getLoginService().getUniqueId().toString().split("-")[0] + " " + cloudPlayer.getLoginService().getServerName() : null) +
                                                " | " +
                                                (cloudPlayer.getConnectedService() != null ?
                                                        cloudPlayer.getConnectedService().getUniqueId().toString().split("-")[0] + " " + cloudPlayer.getConnectedService().getServerName() : null)
                                        );
                                    }
                                },
                                subCommand -> subCommand.enableProperties().appendUsage("| --force")
                        )
                        .removeLastPrefix()

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    sender.sendMessage("=> Registered: " + playerManager.getRegisteredCount());
                                    if (!properties.containsKey("force") && playerManager.getRegisteredCount() > MAX_REGISTERED_PLAYERS_FOR_COMPLETION) {
                                        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-players-too-many-players"));
                                        return;
                                    }

                                    for (ICloudOfflinePlayer cloudPlayer : playerManager.getRegisteredPlayers()) {
                                        sender.sendMessage("- " + cloudPlayer.getUniqueId() + " " + cloudPlayer.getName() + " | Last login: " + DATE_FORMAT.format(new Date(cloudPlayer.getLastLoginTimeMillis())));
                                    }
                                },
                                subCommand -> subCommand.enableProperties().appendUsage("| --force"),
                                anyStringIgnoreCase("registered", "all")
                        )

                        .prefix(anyStringIgnoreCase("player", "pl"))

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    String name = (String) args.argument("name").get();
                                    Collection<? extends ICloudOfflinePlayer> players = playerManager.getOnlinePlayers(name);

                                    if (players.isEmpty()) {
                                        players = playerManager.getOfflinePlayers(name);
                                    }

                                    for (ICloudOfflinePlayer player : players) {
                                        displayPlayer(sender, player);
                                    }
                                },
                                dynamicString(
                                        "name",
                                        LanguageManager.getMessage("module-bridge-command-players-player-not-registered"),
                                        name -> !playerManager.getOfflinePlayers(name).isEmpty(),
                                        () -> playerManager.getRegisteredCount() <= MAX_REGISTERED_PLAYERS_FOR_COMPLETION ?
                                                playerManager.getRegisteredPlayers().stream()
                                                        .map(ICloudOfflinePlayer::getName)
                                                        .collect(Collectors.toList()) :
                                                null
                                )
                        )

                        .prefix(dynamicString(
                                "name",
                                LanguageManager.getMessage("module-bridge-command-players-player-not-online"),
                                name -> !playerManager.getOnlinePlayers(name).isEmpty(),
                                () -> playerManager.getRegisteredCount() <= MAX_ONLINE_PLAYERS_FOR_COMPLETION ? playerManager.onlinePlayers().asNames() : null
                        ))

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    String reason = (String) args.argument("reason").orElse("no reason given");
                                    for (ICloudPlayer player : playerManager.getOnlinePlayers((String) args.argument("name").get())) {
                                        playerManager.getPlayerExecutor(player).kick(reason);
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
                                    for (ICloudPlayer player : playerManager.getOnlinePlayers((String) args.argument("name").get())) {
                                        playerManager.getPlayerExecutor(player).sendChatMessage((String) args.argument("message").orElseThrow(() -> new IllegalArgumentException("No message given")));
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
                                    for (ICloudPlayer player : playerManager.getOnlinePlayers((String) args.argument("name").get())) {
                                        playerManager.getPlayerExecutor(player).connect(server);
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