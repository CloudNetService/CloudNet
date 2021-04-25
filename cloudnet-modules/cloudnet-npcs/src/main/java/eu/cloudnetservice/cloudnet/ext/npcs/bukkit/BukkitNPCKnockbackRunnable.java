package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;

import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BukkitNPCKnockbackRunnable implements Runnable {

    private final BukkitNPCManagement bukkitNPCManagement;
    private final Map<WorldPosition, Location> npcLocations = new HashMap<>();

    public BukkitNPCKnockbackRunnable(BukkitNPCManagement bukkitNPCManagement) {
        this.bukkitNPCManagement = bukkitNPCManagement;
    }

    @Override
    public void run() {
        Set<CloudNPC> npcs = this.bukkitNPCManagement.getCloudNPCS();

        for (CloudNPC npc : npcs) {
            Location npcLocation = this.npcLocations.computeIfAbsent(npc.getPosition(), this.bukkitNPCManagement::toLocation);

            if (npcLocation != null && npcLocation.getWorld() != null) {
                double knockbackDistance = this.bukkitNPCManagement.getOwnNPCConfigurationEntry().getKnockbackDistance();

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
                        });
            }
        }
    }
}
