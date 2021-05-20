package eu.cloudnetservice.cloudnet.ext.signs.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWallSign;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.TextFormat;
import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.service.AbstractServiceSignManagement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class NukkitSignManagement extends AbstractServiceSignManagement<BlockEntitySign> implements SignManagement {

    protected final Plugin plugin;

    public NukkitSignManagement(Plugin plugin) {
        this.plugin = plugin;
    }

    public static NukkitSignManagement getDefaultInstance() {
        return (NukkitSignManagement) CloudNetDriver.getInstance().getServicesRegistry().getFirstService(SignManagement.class);
    }

    @Override
    protected void pushUpdates(@NotNull Set<Sign> signs, @NotNull SignLayout layout) {
        if (Server.getInstance().isPrimaryThread()) {
            this.pushUpdates0(signs, layout);
        } else if (this.plugin.isEnabled()) {
            Server.getInstance().getScheduler().scheduleTask(this.plugin, () -> this.pushUpdates0(signs, layout));
        }
    }

    protected void pushUpdates0(@NotNull Set<Sign> signs, @NotNull SignLayout layout) {
        for (Sign sign : signs) {
            this.pushUpdate(sign, layout);
        }
    }

    @Override
    protected void pushUpdate(@NotNull Sign sign, @NotNull SignLayout layout) {
        if (Server.getInstance().isPrimaryThread()) {
            this.pushUpdate0(sign, layout);
        } else if (this.plugin.isEnabled()) {
            Server.getInstance().getScheduler().scheduleTask(this.plugin, () -> this.pushUpdate0(sign, layout));
        }
    }

    protected void pushUpdate0(@NotNull Sign sign, @NotNull SignLayout layout) {
        Location location = this.locationFromWorldPosition(sign.getWorldPosition());
        if (location != null && location.getLevel().isChunkLoaded(location.getChunkX(), location.getChunkZ())) {
            BlockEntity blockEntity = location.getLevel().getBlockEntity(location);
            if (blockEntity instanceof BlockEntitySign) {
                BlockEntitySign entitySign = (BlockEntitySign) blockEntity;

                String[] lines = this.replaceLines(sign, layout);
                if (lines != null) {
                    for (int i = 0; i < 4; i++) {
                        lines[i] = TextFormat.colorize('&', lines[i]);
                    }

                    entitySign.setText(lines);
                    this.changeBlock(entitySign.getBlock(), layout);
                }
            }
        }
    }

    protected void changeBlock(@NotNull Block block, @NotNull SignLayout layout) {
        Integer itemId = layout.getBlockMaterial() == null ? null : Ints.tryParse(layout.getBlockMaterial());
        if (itemId != null && block instanceof Faceable) {
            BlockFace face = block instanceof BlockWallSign ? ((Faceable) block).getBlockFace().getOpposite() : BlockFace.DOWN;

            Location backLocation = block.getSide(face).getLocation();
            backLocation.getLevel().setBlock(backLocation, Block.get((int) itemId, Math.max(0, layout.getBlockSubId())), true, true);
        }
    }

    @Override
    public @Nullable Sign getSignAt(@NotNull BlockEntitySign blockEntitySign) {
        return this.getSignAt(this.locationToWorldPosition(blockEntitySign.getLocation()));
    }

    @Override
    public @Nullable Sign createSign(@NotNull BlockEntitySign blockEntitySign, @NotNull String group) {
        return this.createSign(blockEntitySign, group, null);
    }

    @Override
    public @Nullable Sign createSign(@NotNull BlockEntitySign blockEntitySign, @NotNull String group, @Nullable String templatePath) {
        SignConfigurationEntry entry = this.getApplicableSignConfigurationEntry();
        if (entry != null) {
            Sign sign = new Sign(group, entry.getTargetGroup(), templatePath, this.locationToWorldPosition(blockEntitySign.getLocation()));
            this.createSign(sign);
            return sign;
        }
        return null;
    }

    @Override
    public void deleteSign(@NotNull BlockEntitySign blockEntitySign) {
        this.deleteSign(this.locationToWorldPosition(blockEntitySign.getLocation()));
    }

    @Override
    public int removeMissingSigns() {
        int removed = 0;
        for (WorldPosition position : this.signs.keySet()) {
            Location location = this.locationFromWorldPosition(position);
            if (location == null || !(location.getLevel().getBlockEntity(location) instanceof BlockEntitySign)) {
                this.deleteSign(position);
                removed++;
            }
        }
        return removed;
    }

    @Override
    protected void startKnockbackTask() {
        Server.getInstance().getScheduler().scheduleDelayedRepeatingTask(this.plugin, () -> {
            SignConfigurationEntry entry = this.getApplicableSignConfigurationEntry();
            if (entry != null) {
                SignConfigurationEntry.KnockbackConfiguration configuration = entry.getKnockbackConfiguration();
                if (configuration.isValidAndEnabled()) {
                    double distance = configuration.getDistance();

                    for (Sign value : this.signs.values()) {
                        Location location = this.locationFromWorldPosition(value.getWorldPosition());
                        if (location != null && location.getLevel().isChunkLoaded(location.getChunkX(), location.getChunkZ())
                                && location.getLevel().getBlockEntity(location) instanceof BlockEntitySign) {
                            AxisAlignedBB axisAlignedBB = new SimpleAxisAlignedBB(location, location)
                                    .expand(distance, distance, distance);

                            for (Entity entity : location.getLevel().getNearbyEntities(axisAlignedBB)) {
                                if (entity instanceof Player && (configuration.getBypassPermission() == null
                                        || !((Player) entity).hasPermission(configuration.getBypassPermission()))) {
                                    Vector3 vector = entity.getPosition()
                                            .subtract(location)
                                            .normalize()
                                            .multiply(configuration.getStrength());
                                    vector.y = 0.2D;
                                    entity.setMotion(vector);
                                }
                            }
                        }
                    }
                }
            }
        }, 0, 5);
    }

    protected @NotNull WorldPosition locationToWorldPosition(@NotNull Location location) {
        return new WorldPosition(location.getX(), location.getY(), location.getZ(), 0, 0, location.getLevel().getName());
    }

    protected @Nullable Location locationFromWorldPosition(@NotNull WorldPosition position) {
        Level level = Server.getInstance().getLevelByName(position.getWorld());
        return level == null ? null : new Location(position.getX(), position.getY(), position.getZ(), level);
    }
}
