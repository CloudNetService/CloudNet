package de.dytanic.cloudnet.ext.signs.nukkit;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.math.Vector3;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.SignPosition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NukkitSignKnockbackRunnable implements Runnable {

    private final Map<SignPosition, Location> signLocations = new HashMap<>();

    private final SignConfigurationEntry ownConfigurationEntry;

    NukkitSignKnockbackRunnable(SignConfigurationEntry ownConfigurationEntry) {
        this.ownConfigurationEntry = ownConfigurationEntry;
    }

    @Override
    public void run() {
        List<Sign> signs = AbstractSignManagement.getInstance().getSigns();

        for (Sign sign : signs) {
            Location signLocation = signLocations.computeIfAbsent(sign.getWorldPosition(), signPosition -> NukkitSignManagement.getInstance().toLocation(signPosition));

            if (signLocation != null && signLocation.getLevel() != null) {
                double knockbackDistance = this.ownConfigurationEntry.getKnockbackDistance();

                AxisAlignedBB boundingBox = new SimpleAxisAlignedBB(signLocation, signLocation).expand(knockbackDistance, knockbackDistance, knockbackDistance);

                Arrays.stream(signLocation.getLevel().getNearbyEntities(boundingBox))
                        .filter(entity -> entity instanceof Player && !((Player) entity).hasPermission("cloudnet.signs.knockback.bypass"))
                        .forEach(player -> {
                            // pushing the player back with the specified strength
                            Vector3 vector3 = player.getPosition().subtract(signLocation)
                                    .normalize()
                                    .multiply(this.ownConfigurationEntry.getKnockbackStrength());
                            vector3.y = 0.2;

                            player.setMotion(vector3);
                        });
            }
        }

    }


}
