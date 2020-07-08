package de.dytanic.cloudnet.ext.bridge.sponge.platform;

import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.network.status.StatusResponse;
import org.spongepowered.api.scheduler.SpongeExecutorService;

import java.util.concurrent.TimeUnit;

public final class SpongeClientServerPingEventCaller {

    private SpongeClientServerPingEventCaller() {
        throw new UnsupportedOperationException();
    }

    private static SpongeExecutorService executorService;

    public static void startCalling(@NotNull Object plugin) {
        if (executorService != null) {
            return;
        }

        executorService = Sponge.getScheduler().createAsyncExecutor(plugin);
        executorService.scheduleAtFixedRate(() -> {
            ClientPingServerEvent clientPingServerEvent = SpongeEventFactory.createClientPingServerEvent(
                    Cause.of(EventContext.empty(), Sponge.getServer()),
                    CloudNetSpongeStatusClient.INSTANCE,
                    new CloudNetSpongeClientPingEventResponse()
            );
            Sponge.getEventManager().post(clientPingServerEvent);

            if (clientPingServerEvent.isCancelled()) {
                return;
            }

            boolean hasToUpdate = false;
            boolean value = false;

            String plainDescription = clientPingServerEvent.getResponse().getDescription().toPlain();
            if (!plainDescription.equalsIgnoreCase(BridgeServerHelper.getMotd())) {
                hasToUpdate = true;
                BridgeServerHelper.setMotd(plainDescription);

                String lowerMotd = plainDescription.toLowerCase();
                if (lowerMotd.contains("running") || lowerMotd.contains("ingame") || lowerMotd.contains("playing")) {
                    value = true;
                }
            }

            int max = clientPingServerEvent.getResponse().getPlayers().map(StatusResponse.Players::getMax).orElse(-1);
            if (max >= 0 && max != BridgeServerHelper.getMaxPlayers()) {
                hasToUpdate = true;
                BridgeServerHelper.setMaxPlayers(max);
            }

            if (value) {
                BridgeServerHelper.changeToIngame(true);
                return;
            }

            if (hasToUpdate) {
                BridgeHelper.updateServiceInfo();
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    public static void closeNow() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }
}
