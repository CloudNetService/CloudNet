package de.dytanic.cloudnet.ext.bridge.bungee.command;

import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public final class CommandHub extends Command {

    public CommandHub() {
        super("hub");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer proxiedPlayer = (ProxiedPlayer) sender;

        if (BungeeCloudNetHelper.isOnAFallbackInstance(proxiedPlayer)) {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', BridgeConfigurationProvider.load().getMessages().get("command-hub-already-in-hub"))));
            return;
        }

        String server = BungeeCloudNetHelper.filterServiceForProxiedPlayer(proxiedPlayer, proxiedPlayer.getServer() != null ?
                proxiedPlayer.getServer().getInfo().getName()
                :
                null);

        if (server != null) {
            ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(server);

            if (serverInfo == null) {
                sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', BridgeConfigurationProvider.load().getMessages().get("command-hub-no-server-found"))));
                return;
            }

            proxiedPlayer.connect(serverInfo);
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                    BridgeConfigurationProvider.load().getMessages().get("command-hub-success-connect"))
                    .replace("%server%", server)
            ));
        } else {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', BridgeConfigurationProvider.load().getMessages().get("command-hub-no-server-found"))));
        }
    }

    @Override
    public String[] getAliases() {
        return new String[]{"lobby", "l", "leave"};
    }
}