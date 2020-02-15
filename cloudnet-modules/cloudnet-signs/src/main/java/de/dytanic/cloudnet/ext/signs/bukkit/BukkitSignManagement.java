package de.dytanic.cloudnet.ext.signs.bukkit;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignLayout;
import de.dytanic.cloudnet.ext.signs.SignPosition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

public final class BukkitSignManagement extends AbstractSignManagement {

    private static BukkitSignManagement instance;
    private final BukkitCloudNetSignsPlugin plugin;

    BukkitSignManagement(BukkitCloudNetSignsPlugin plugin) {
        super();
        instance = this;

        this.plugin = plugin;

        super.executeSearchingTask();
        super.executeStartingTask();
        super.updateSigns();
    }

    public static BukkitSignManagement getInstance() {
        return BukkitSignManagement.instance;
    }


    @Override
    protected void updateSignNext(@NotNull Sign sign, @NotNull SignLayout signLayout, @Nullable ServiceInfoSnapshot serviceInfoSnapshot) {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            Location location = this.toLocation(sign.getWorldPosition());

            if (location != null) {

                Block block = location.getBlock();

                if (block.getState() instanceof org.bukkit.block.Sign) {
                    org.bukkit.block.Sign bukkitSign = (org.bukkit.block.Sign) block.getState();

                    this.updateSign(location, sign, bukkitSign, signLayout, serviceInfoSnapshot);
                }

            }

        });
    }

    @Override
    public void cleanup() {
        Iterator<Sign> signIterator = super.signs.iterator();

        while (signIterator.hasNext()) {
            Sign sign = signIterator.next();

            Location location = this.toLocation(sign.getWorldPosition());

            if (location == null || !(location.getBlock().getState() instanceof org.bukkit.block.Sign)) {
                signIterator.remove();
                super.sendSignRemoveUpdate(sign);
            }
        }
    }

    @Override
    protected void runTaskLater(@NotNull Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(this.plugin, runnable, delay);
    }

    private void updateSign(Location location, Sign sign, org.bukkit.block.Sign bukkitSign, SignLayout signLayout, ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(location);
        Preconditions.checkNotNull(bukkitSign);
        Preconditions.checkNotNull(signLayout);

        if (signLayout.getLines() != null &&
                signLayout.getLines().length == 4) {

            for (int i = 0; i < 4; i++) {
                String line = ChatColor.translateAlternateColorCodes('&', super.addDataToLine(sign, signLayout.getLines()[i], serviceInfoSnapshot));
                bukkitSign.setLine(i, line);
            }

            bukkitSign.update();

            this.changeBlock(location, signLayout.getBlockType(), signLayout.getSubId());
        }
    }

    private void changeBlock(Location location, String blockType, int subId) {
        Preconditions.checkNotNull(location);

        if (blockType != null && subId != -1) {

            BlockState signBlockState = location.getBlock().getState();
            MaterialData signMaterialData = signBlockState.getData();

            BlockFace signBlockFace;

            if (signMaterialData instanceof org.bukkit.material.Sign) { // will return false on 1.14+, even if it's a wall sign
                org.bukkit.material.Sign sign = (org.bukkit.material.Sign) signMaterialData;

                signBlockFace = sign.isWallSign() ? sign.getFacing() : BlockFace.UP;
            } else { // trying to get the facing over directionals from the 1.13+ api
                signBlockFace = this.getSignFacing(signBlockState);
            }

            if (signBlockFace != null) {

                BlockState backBlockState = location.getBlock().getRelative(signBlockFace.getOppositeFace()).getState();
                Material backBlockMaterial = Material.getMaterial(blockType.toUpperCase());

                if (backBlockMaterial != null) {
                    backBlockState.setType(backBlockMaterial);
                    backBlockState.setData(new MaterialData(backBlockMaterial, (byte) subId));
                    backBlockState.update(true);
                }

            }

        }
    }

    /**
     * Returns the facing of the specified block state, if its block data is an {@link org.bukkit.block.data.type.WallSign}
     * from the 1.13+ spigot api
     *
     * @param blockState the block state the facing should be returned from
     * @return the facing of the block state
     */
    private BlockFace getSignFacing(BlockState blockState) {
        try {

            Method getBlockDataMethod = BlockState.class.getDeclaredMethod("getBlockData");
            Object blockData = getBlockDataMethod.invoke(blockState);

            Class<?> wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");

            if (wallSignClass.isInstance(blockData)) {
                Method getFacingMethod = wallSignClass.getMethod("getFacing");

                return (BlockFace) getFacingMethod.invoke(blockData);
            }

            return BlockFace.UP;

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return null;
        }
    }

    public Location toLocation(SignPosition signPosition) {
        Preconditions.checkNotNull(signPosition);

        return Bukkit.getWorld(signPosition.getWorld()) != null ? new Location(
                Bukkit.getWorld(signPosition.getWorld()),
                signPosition.getX(),
                signPosition.getY(),
                signPosition.getZ()
        ) : null;
    }

    public BukkitCloudNetSignsPlugin getPlugin() {
        return this.plugin;
    }


}