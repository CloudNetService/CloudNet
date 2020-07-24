package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.labymod;


import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.npcs.AbstractNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public class LabyModEmotePlayer implements Listener {

    private final JavaPlugin javaPlugin;

    private final AbstractNPCManagement abstractNPCManagement;

    private final IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);

    public LabyModEmotePlayer(JavaPlugin javaPlugin, AbstractNPCManagement abstractNPCManagement) {
        this.javaPlugin = javaPlugin;
        this.abstractNPCManagement = abstractNPCManagement;

        this.schedule();
    }

    private void schedule() {
        NPCConfigurationEntry.LabyModEmotes labyModEmotes = this.abstractNPCManagement.getOwnNPCConfigurationEntry().getLabyModEmotes();

        long minTicks = labyModEmotes.getMinEmoteDelayTicks();
        long maxTicks = labyModEmotes.getMaxEmoteDelayTicks();

        Preconditions.checkArgument(minTicks > 0, "EmoteDelayTicks have to be > 0!");
        Preconditions.checkArgument(maxTicks > 0, "EmoteDelayTicks have to be > 0!");
        Preconditions.checkArgument(maxTicks >= minTicks, "MinEmoteDelayTicks cannot be greater than MaxEmoteDelayTicks!");

        long emotePlayDelayTicks = minTicks == maxTicks
                ? minTicks
                : ThreadLocalRandom.current().nextLong(labyModEmotes.getMinEmoteDelayTicks(), labyModEmotes.getMaxEmoteDelayTicks());

        if (this.javaPlugin.isEnabled() && labyModEmotes.getEmoteIds().length > 0) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(this.javaPlugin, () -> {
                byte[] channelMessage = this.createChannelMessage(labyModEmotes.getEmoteIds());

                Bukkit.getOnlinePlayers().forEach(player ->
                        this.playerManager.getPlayerExecutor(player.getUniqueId()).sendPluginMessage("LMC", channelMessage));

                this.schedule();
            }, emotePlayDelayTicks);
        }
    }

    private byte[] createChannelMessage(int[] allEmoteIds) {
        int emoteId = this.abstractNPCManagement.getOwnNPCConfigurationEntry().getLabyModEmotes().isPlayEmotesSynchronous()
                ? this.getRandomEmoteId(allEmoteIds)
                : -1;

        JsonArray emoteList = new JsonArray();

        for (CloudNPC cloudNPC : this.abstractNPCManagement.getCloudNPCS()) {
            JsonObject emote = new JsonObject();
            emote.addProperty("uuid", cloudNPC.getUUID().toString());
            emote.addProperty("emote_id", emoteId == -1 ? this.getRandomEmoteId(allEmoteIds) : emoteId);

            emoteList.add(emote);
        }

        return LabyModChannelUtil.createChannelMessageData("emote_api", emoteList);
    }

    private int getRandomEmoteId(int[] emoteIds) {
        return emoteIds[ThreadLocalRandom.current().nextInt(emoteIds.length)];
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void handleJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        int[] onJoinEmoteIds = this.abstractNPCManagement
                .getOwnNPCConfigurationEntry()
                .getLabyModEmotes()
                .getOnJoinEmoteIds();

        if (onJoinEmoteIds.length > 0) {
            byte[] channelMessage = this.createChannelMessage(onJoinEmoteIds);

            Bukkit.getScheduler().runTaskLater(this.javaPlugin, () ->
                            this.playerManager.getPlayerExecutor(player.getUniqueId()).sendPluginMessage("LMC", channelMessage),
                    40
            );
        }
    }

}
