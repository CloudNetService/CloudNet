package de.dytanic.cloudnet.ext.signs.nukkit;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWallSign;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.level.Location;
import cn.nukkit.math.BlockFace;
import cn.nukkit.utils.Faceable;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public final class NukkitSignManagement extends AbstractSignManagement {

    private static NukkitSignManagement instance;
    private final NukkitCloudNetSignsPlugin plugin;

    NukkitSignManagement(NukkitCloudNetSignsPlugin plugin) {
        super();
        instance = this;

        this.plugin = plugin;

        super.executeSearchingTask();
        super.executeStartingTask();
        super.updateSigns();
    }

    public static NukkitSignManagement getInstance() {
        return NukkitSignManagement.instance;
    }


    @Override
    protected void updateSignNext(@NotNull Sign sign, @NotNull SignLayout signLayout, @Nullable ServiceInfoSnapshot serviceInfoSnapshot) {
        Server.getInstance().getScheduler().scheduleTask(this.plugin, () -> {
            Location location = this.toLocation(sign.getWorldPosition());

            if (location != null) {

                BlockEntity blockEntity = location.getLevel().getBlockEntity(location);

                if (blockEntity instanceof BlockEntitySign) {
                    this.updateSign(sign, (BlockEntitySign) blockEntity, signLayout, serviceInfoSnapshot);
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

            if (location == null || !(location.getLevel().getBlockEntity(location) instanceof BlockEntitySign)) {
                signIterator.remove();
                super.sendSignRemoveUpdate(sign);
            }
        }
    }

    @Override
    protected void runTaskLater(@NotNull Runnable runnable, long delay) {
        Server.getInstance().getScheduler().scheduleDelayedTask(this.plugin, runnable, Math.toIntExact(delay));
    }

    private void updateSign(Sign sign, BlockEntitySign nukkitSign, SignLayout signLayout, ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(nukkitSign);
        Preconditions.checkNotNull(signLayout);

        if (signLayout.getLines() != null &&
                signLayout.getLines().length == 4) {

            String[] lines = new String[4];

            for (int i = 0; i < lines.length; i++) {
                String line = super.addDataToLine(sign, signLayout.getLines()[i], serviceInfoSnapshot).replace('&', '§');
                lines[i] = line;
            }

            nukkitSign.setText(lines);

            this.changeBlock(nukkitSign, signLayout.getBlockType(), signLayout.getSubId());
        }
    }

    private void changeBlock(BlockEntitySign nukkitSign, String blockType, int subId) {
        Preconditions.checkNotNull(nukkitSign);

        if (blockType != null && subId != -1) {

            Block block = nukkitSign.getBlock();

            if (block instanceof Faceable) {
                Faceable faceable = (Faceable) block;
                BlockFace blockFace = block instanceof BlockWallSign ? faceable.getBlockFace().getOpposite() : BlockFace.DOWN;

                Location backBlockLocation = block.getSide(blockFace).getLocation();

                int itemId;
                try {
                    itemId = Integer.parseInt(blockType);
                } catch (NumberFormatException exception) {
                    return;
                }

                backBlockLocation.getLevel().setBlock(backBlockLocation, Block.get(itemId, subId), true, true);
            }


        }
    }

    public Location toLocation(WorldPosition worldPosition) {
        Preconditions.checkNotNull(worldPosition);

        return Server.getInstance().getLevelByName(worldPosition.getWorld()) != null ? new Location(
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                Server.getInstance().getLevelByName(worldPosition.getWorld())
        ) : null;
    }

    public NukkitCloudNetSignsPlugin getPlugin() {
        return this.plugin;
    }


}
