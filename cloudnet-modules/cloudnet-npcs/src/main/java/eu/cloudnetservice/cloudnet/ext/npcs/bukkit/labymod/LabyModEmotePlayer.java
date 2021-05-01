package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.labymod;

import com.github.juliarn.npc.NPC;
import com.github.juliarn.npc.modifier.LabyModModifier;
import com.google.common.base.Preconditions;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCManagement;
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

    private final BukkitNPCManagement npcManagement;

    public LabyModEmotePlayer(JavaPlugin javaPlugin, BukkitNPCManagement npcManagement) {
        this.javaPlugin = javaPlugin;
        this.npcManagement = npcManagement;

        this.schedule();
    }

    private void schedule() {
        NPCConfigurationEntry.LabyModEmotes labyModEmotes = this.npcManagement.getOwnNPCConfigurationEntry().getLabyModEmotes();

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
                int emoteId = this.getEmoteId(labyModEmotes.getEmoteIds());

                for (NPC npc : this.npcManagement.getNPCPool().getNPCs()) {
                    if (emoteId == -1) {
                        emoteId = this.getRandomEmoteId(labyModEmotes.getEmoteIds());
                    }
                    npc.labymod().queue(LabyModModifier.LabyModAction.EMOTE, emoteId).send();
                }

                this.schedule();
            }, emotePlayDelayTicks);
        }
    }

    private int getEmoteId(int[] emoteIds) {
        return this.npcManagement.getOwnNPCConfigurationEntry().getLabyModEmotes().isPlayEmotesSynchronous()
                ? this.getRandomEmoteId(emoteIds)
                : -1;
    }

    private int getRandomEmoteId(int[] emoteIds) {
        return emoteIds[ThreadLocalRandom.current().nextInt(emoteIds.length)];
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void handleJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        int[] onJoinEmoteIds = this.npcManagement
                .getOwnNPCConfigurationEntry()
                .getLabyModEmotes()
                .getOnJoinEmoteIds();

        if (onJoinEmoteIds.length > 0) {
            int emoteId = this.getEmoteId(onJoinEmoteIds);

            for (NPC npc : this.npcManagement.getNPCPool().getNPCs()) {
                if (emoteId == -1) {
                    emoteId = this.getRandomEmoteId(onJoinEmoteIds);
                }
                npc.labymod().queue(LabyModModifier.LabyModAction.EMOTE, emoteId).send(player);
            }
        }
    }
}
