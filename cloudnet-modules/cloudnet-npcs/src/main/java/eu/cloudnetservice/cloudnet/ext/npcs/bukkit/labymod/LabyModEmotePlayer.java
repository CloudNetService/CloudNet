package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.labymod;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.npcs.AbstractNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public class LabyModEmotePlayer {

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
        long emotePlayDelayTicks = ThreadLocalRandom.current().nextLong(labyModEmotes.getMinEmoteDelayTicks(), labyModEmotes.getMaxEmoteDelayTicks());

        if (this.javaPlugin.isEnabled() && labyModEmotes.getEmoteIds().length > 0) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(this.javaPlugin, () -> {
                int emoteId = labyModEmotes.isPlayEmotesSynchronous() ? this.getRandomEmoteId(labyModEmotes) : -1;
                JsonArray emoteList = new JsonArray();

                for (CloudNPC cloudNPC : this.abstractNPCManagement.getCloudNPCS()) {
                    JsonObject emote = new JsonObject();
                    emote.addProperty("uuid", cloudNPC.getUUID().toString());
                    emote.addProperty("emote_id", emoteId == -1 ? this.getRandomEmoteId(labyModEmotes) : emoteId);

                    emoteList.add(emote);
                }

                byte[] channelMessage = LabyModChannelUtil.createChannelMessageData("emote_api", emoteList);

                Bukkit.getOnlinePlayers().forEach(player ->
                        this.playerManager.getPlayerExecutor(player.getUniqueId()).sendPluginMessage("LMC", channelMessage));

                this.schedule();
            }, emotePlayDelayTicks);
        }
    }

    private int getRandomEmoteId(NPCConfigurationEntry.LabyModEmotes labyModEmotes) {
        return labyModEmotes.getEmoteIds()[ThreadLocalRandom.current().nextInt(labyModEmotes.getEmoteIds().length)];
    }

}
