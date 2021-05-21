package eu.cloudnetservice.cloudnet.ext.signs.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.service.AbstractServiceSignManagement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class BukkitSignManagement extends AbstractServiceSignManagement<org.bukkit.block.Sign> implements SignManagement {

    protected final Plugin plugin;

    protected BukkitSignManagement(Plugin plugin) {
        this.plugin = plugin;
    }

    public static BukkitSignManagement getDefaultInstance() {
        return (BukkitSignManagement) CloudNetDriver.getInstance().getServicesRegistry().getFirstService(SignManagement.class);
    }

    @Override
    protected void pushUpdates(@NotNull Set<Sign> signs, @NotNull SignLayout layout) {
        if (Bukkit.isPrimaryThread()) {
            this.pushUpdates0(signs, layout);
        } else if (this.plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(this.plugin, () -> this.pushUpdates0(signs, layout));
        }
    }

    protected void pushUpdates0(@NotNull Set<Sign> signs, @NotNull SignLayout layout) {
        for (Sign sign : signs) {
            this.pushUpdate(sign, layout);
        }
    }

    @Override
    protected void pushUpdate(@NotNull Sign sign, @NotNull SignLayout layout) {
        if (Bukkit.isPrimaryThread()) {
            this.pushUpdate0(sign, layout);
        } else if (this.plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(this.plugin, () -> this.pushUpdate0(sign, layout));
        }
    }

    protected void pushUpdate0(@NotNull Sign sign, @NotNull SignLayout layout) {
        Location location = this.worldPositionToLocation(sign.getWorldPosition());
        if (location != null) {
            int chunkX = (int) Math.floor(location.getX()) >> 4;
            int chunkZ = (int) Math.floor(location.getZ()) >> 4;

            if (location.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                Block block = location.getBlock();
                if (block.getState() instanceof org.bukkit.block.Sign) {
                    org.bukkit.block.Sign bukkitSign = (org.bukkit.block.Sign) block.getState();

                    String[] replaced = this.replaceLines(sign, layout);
                    if (replaced != null) {
                        for (int i = 0; i < 4; i++) {
                            bukkitSign.setLine(i, replaced[i]);
                        }

                        bukkitSign.update();
                    }

                    this.changeBlock(block, layout);
                }
            }
        }
    }

    @SuppressWarnings("deprecation") // legacy 1.8 support...
    protected void changeBlock(@NotNull Block block, @NotNull SignLayout layout) {
        Material material = layout.getBlockMaterial() == null ? null : Material.getMaterial(layout.getBlockMaterial().toUpperCase());
        if (material != null && material.isBlock()) {
            BlockFace face = BukkitCompatibility.getFacing(block.getState());
            if (face != null) {
                BlockState behind = block.getRelative(face.getOppositeFace()).getState();
                if (layout.getBlockSubId() >= 0) {
                    behind.setData(new MaterialData(material, (byte) layout.getBlockSubId()));
                } else {
                    behind.setType(material);
                }

                behind.update(true);
            }
        }
    }

    @Override
    public @Nullable Sign getSignAt(@NotNull org.bukkit.block.Sign sign) {
        return this.getSignAt(this.locationToWorldPosition(sign.getLocation()));
    }

    @Override
    public @Nullable Sign createSign(@NotNull org.bukkit.block.Sign sign, @NotNull String group) {
        return this.createSign(sign, group, null);
    }

    @Override
    public @Nullable Sign createSign(@NotNull org.bukkit.block.Sign sign, @NotNull String group, @Nullable String templatePath) {
        SignConfigurationEntry entry = this.getApplicableSignConfigurationEntry();
        if (entry != null) {
            Sign created = new Sign(group, entry.getTargetGroup(), templatePath, this.locationToWorldPosition(sign.getLocation(), entry.getTargetGroup()));
            this.createSign(created);
            return created;
        }
        return null;
    }

    @Override
    public void deleteSign(@NotNull org.bukkit.block.Sign sign) {
        this.deleteSign(this.locationToWorldPosition(sign.getLocation()));
    }

    @Override
    public int removeMissingSigns() {
        int removed = 0;
        for (WorldPosition position : this.signs.keySet()) {
            Location location = this.worldPositionToLocation(position);
            if (location == null || !(location.getBlock().getState() instanceof org.bukkit.block.Sign)) {
                this.deleteSign(position);
                removed++;
            }
        }

        return removed;
    }

    @Override
    protected void startKnockbackTask() {
        Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            SignConfigurationEntry entry = this.getApplicableSignConfigurationEntry();
            if (entry != null) {
                SignConfigurationEntry.KnockbackConfiguration configuration = entry.getKnockbackConfiguration();
                if (configuration.isValidAndEnabled()) {
                    double distance = configuration.getDistance();

                    for (Sign value : this.signs.values()) {
                        Location location = this.worldPositionToLocation(value.getWorldPosition());
                        if (location != null) {
                            int chunkX = (int) Math.floor(location.getX()) >> 4;
                            int chunkZ = (int) Math.floor(location.getZ()) >> 4;

                            if (location.getWorld().isChunkLoaded(chunkX, chunkZ)
                                    && location.getBlock().getState() instanceof org.bukkit.block.Sign) {
                                location.getWorld()
                                        .getNearbyEntities(location, distance, distance, distance)
                                        .stream()
                                        .filter(entity -> entity instanceof Player && (configuration.getBypassPermission() == null
                                                || !entity.hasPermission(configuration.getBypassPermission())))
                                        .forEach(entity -> entity.setVelocity(entity.getLocation().toVector()
                                                .subtract(location.toVector())
                                                .normalize()
                                                .multiply(configuration.getStrength())));
                            }
                        }
                    }
                }
            }
        }, 0, 5);
    }

    protected @NotNull WorldPosition locationToWorldPosition(@NotNull Location location) {
        return new WorldPosition(location.getX(), location.getY(), location.getZ(), 0, 0, location.getWorld().getName());
    }

    protected @NotNull WorldPosition locationToWorldPosition(@NotNull Location location, @NotNull String group) {
        return new WorldPosition(location.getX(), location.getY(), location.getZ(), 0, 0, location.getWorld().getName(), group);
    }

    protected @Nullable Location worldPositionToLocation(@NotNull WorldPosition position) {
        World world = Bukkit.getWorld(position.getWorld());
        return world == null ? null : new Location(world, position.getX(), position.getY(), position.getZ());
    }
}
