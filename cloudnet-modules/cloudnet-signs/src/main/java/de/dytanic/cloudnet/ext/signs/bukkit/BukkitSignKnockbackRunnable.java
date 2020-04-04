package de.dytanic.cloudnet.ext.signs.bukkit;


import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BukkitSignKnockbackRunnable implements Runnable {

    private final Map<WorldPosition, Location> signLocations = new HashMap<>();

    private final SignConfigurationEntry ownConfigurationEntry;

    BukkitSignKnockbackRunnable(SignConfigurationEntry ownConfigurationEntry) {
        this.ownConfigurationEntry = ownConfigurationEntry;
    }

    @Override
    public void run() {
        Set<Sign> signs = AbstractSignManagement.getInstance().getSigns();

        for (Sign sign : signs) {
            Location signLocation = signLocations.computeIfAbsent(sign.getWorldPosition(), signPosition -> BukkitSignManagement.getInstance().toLocation(signPosition));

            if (signLocation != null && signLocation.getWorld() != null) {
                double knockbackDistance = this.ownConfigurationEntry.getKnockbackDistance();

                signLocation.getWorld()
                        .getNearbyEntities(signLocation, knockbackDistance, knockbackDistance, knockbackDistance)
                        .stream()
                        .filter(entity -> entity instanceof Player && !entity.hasPermission("cloudnet.signs.knockback.bypass"))
                        .forEach(player -> {
                            // pushing the player back with the specified strength
                            player.setVelocity(player.getLocation().toVector().subtract(signLocation.toVector())
                                    .normalize()
                                    .multiply(this.ownConfigurationEntry.getKnockbackStrength())
                                    .setY(0.2));
                        });
            }
        }

    }

}
