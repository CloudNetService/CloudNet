package de.dytanic.cloudnet.ext.bridge.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.player.executor.ServerSelectorType;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public abstract class PlayerExecutorListener<Player> {

    @EventListener
    public void handleChannelMessage(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equals(BridgeConstants.BRIDGE_PLAYER_API_CHANNEL) || event.getMessage() == null) {
            return;
        }

        if (event.getMessage().equals("broadcast_message_component")) {
            String data = event.getBuffer().readString();
            String permission = event.getBuffer().readOptionalString();

            this.broadcastMessageComponent(data, permission);
            return;
        }

        UUID uniqueId = event.getBuffer().readUUID();

        Collection<Player> players;
        if (uniqueId.getLeastSignificantBits() == 0 && uniqueId.getMostSignificantBits() == 0) {
            players = this.getOnlinePlayers();
        } else {
            Player player = this.getPlayer(uniqueId);
            if (player == null) {
                return;
            }
            players = Collections.singletonList(player);
        }

        switch (event.getMessage()) {
            case "connect_server": {
                String service = event.getBuffer().readString();

                for (Player player : players) {
                    this.connect(player, service);
                }
            }
            break;

            case "connect_type": {
                ServerSelectorType selectorType = event.getBuffer().readEnumConstant(ServerSelectorType.class);

                this.connect(players, serviceInfoSnapshot -> true, selectorType);
            }
            break;

            case "connect_fallback": {
                for (Player player : players) {
                    this.connectToFallback(player);
                }
            }
            break;

            case "connect_group": {
                String group = event.getBuffer().readString();
                ServerSelectorType selectorType = event.getBuffer().readEnumConstant(ServerSelectorType.class);

                this.connect(players, serviceInfoSnapshot -> serviceInfoSnapshot.getConfiguration().hasGroup(group), selectorType);
            }
            break;

            case "connect_task": {
                String task = event.getBuffer().readString();
                ServerSelectorType selectorType = event.getBuffer().readEnumConstant(ServerSelectorType.class);

                this.connect(players, serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getTaskName().equalsIgnoreCase(task), selectorType);
            }
            break;

            case "kick": {
                String reason = event.getBuffer().readString();

                for (Player player : players) {
                    this.kick(player, reason.replace('&', '§'));
                }
            }
            break;

            case "send_message": {
                String message = event.getBuffer().readString();

                for (Player player : players) {
                    this.sendMessage(player, message);
                }
            }
            break;
            case "send_message_component": {
                String data = event.getBuffer().readString();

                for (Player player : players) {
                    this.sendMessageComponent(player, data);
                }
            }
            break;

            case "send_plugin_message": {
                String tag = event.getBuffer().readString();
                byte[] data = event.getBuffer().readArray();

                for (Player player : players) {
                    this.sendPluginMessage(player, tag, data);
                }
            }
            break;

            case "dispatch_proxy_command": {
                String command = event.getBuffer().readString();

                for (Player player : players) {
                    this.dispatchCommand(player, command);
                }
            }

        }
    }

    @Nullable
    protected abstract Player getPlayer(@NotNull UUID uniqueId);

    @NotNull
    protected abstract Collection<Player> getOnlinePlayers();

    protected abstract void connect(@NotNull Player player, @NotNull String service);

    protected abstract void kick(@NotNull Player player, @NotNull String reason);

    protected abstract void sendMessage(@NotNull Player player, @NotNull String message);

    protected abstract void sendMessageComponent(@NotNull Player player, @NotNull String data);

    protected abstract void sendPluginMessage(@NotNull Player player, @NotNull String tag, @NotNull byte[] data);

    protected abstract void broadcastMessageComponent(@NotNull String data, @Nullable String permission);

    protected abstract void broadcastMessage(@NotNull String message, @Nullable String permission);

    protected abstract void connectToFallback(@NotNull Player player);

    protected abstract void dispatchCommand(@NotNull Player player, @NotNull String command);

    protected Optional<ServiceInfoSnapshot> findService(@NotNull Predicate<ServiceInfoSnapshot> filter, @NotNull ServerSelectorType selectorType) {
        return BridgeProxyHelper.getCachedServiceInfoSnapshots().stream()
                .filter(filter)
                .min(selectorType.getComparator());
    }

    protected void connect(@NotNull Collection<Player> players, @NotNull Predicate<ServiceInfoSnapshot> filter, @NotNull ServerSelectorType selectorType) {
        this.findService(filter, selectorType).map(ServiceInfoSnapshot::getName).ifPresent(service -> {
            for (Player player : players) {
                this.connect(player, service);
            }
        });
    }

}
