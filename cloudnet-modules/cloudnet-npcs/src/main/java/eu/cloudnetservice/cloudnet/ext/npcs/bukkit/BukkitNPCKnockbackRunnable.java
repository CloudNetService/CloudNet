package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;

import com.github.juliarn.npc.modifier.LabyModModifier;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class BukkitNPCKnockbackRunnable implements Runnable {

    private final BukkitNPCManagement bukkitNPCManagement;
    private final Map<WorldPosition, Location> npcLocations = new HashMap<>();
    private final Random random = new Random();

    public BukkitNPCKnockbackRunnable(BukkitNPCManagement bukkitNPCManagement) {
        this.bukkitNPCManagement = bukkitNPCManagement;
    }

    @Override
    public void run() {
        Set<CloudNPC> npcs = this.bukkitNPCManagement.getCloudNPCS();

        for (CloudNPC cloudNpc : npcs) {
            Location npcLocation = this.npcLocations.computeIfAbsent(cloudNpc.getPosition(), this.bukkitNPCManagement::toLocation);

            if (npcLocation != null && npcLocation.getWorld() != null) {
                double knockbackDistance = this.bukkitNPCManagement.getOwnNPCConfigurationEntry().getKnockbackDistance();

                int randomEmoteId = this.getRandomEmoteId();

                npcLocation.getWorld()
                        .getNearbyEntities(npcLocation, knockbackDistance, knockbackDistance, knockbackDistance)
                        .stream()
                        .filter(entity -> entity instanceof Player && !entity.hasPermission("cloudnet.npcs.knockback.bypass"))
                        .forEach(player -> {
                            // pushing the player back with the specified strength
                            player.setVelocity(player.getLocation().toVector().subtract(npcLocation.toVector())
                                    .normalize()
                                    .multiply(this.bukkitNPCManagement.getOwnNPCConfigurationEntry().getKnockbackStrength())
                                    .setY(0.2));

                            if (randomEmoteId != -1) {
                                this.bukkitNPCManagement.getNPCPool().getNpc(cloudNpc.getUUID()).ifPresent(npc ->
                                        npc.labymod().queue(LabyModModifier.LabyModAction.EMOTE, randomEmoteId).send((Player) player));
                            }
                        });
            }
        }
    }

    private int getRandomEmoteId() {
        int[] onKnockbackEmoteIds = this.bukkitNPCManagement.getOwnNPCConfigurationEntry()
                .getLabyModEmotes()
                .getOnKnockbackEmoteIds();

        if (onKnockbackEmoteIds.length == 0) {
            return -1;
        }

        return onKnockbackEmoteIds[this.random.nextInt(onKnockbackEmoteIds.length)];
    }
}
