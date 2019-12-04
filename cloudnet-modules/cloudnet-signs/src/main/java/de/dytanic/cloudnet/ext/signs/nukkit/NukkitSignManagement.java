package de.dytanic.cloudnet.ext.signs.nukkit;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWallSign;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.level.Location;
import cn.nukkit.math.BlockFace;
import cn.nukkit.utils.Faceable;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignLayout;
import de.dytanic.cloudnet.ext.signs.SignPosition;

public final class NukkitSignManagement extends AbstractSignManagement {

    private static NukkitSignManagement instance;
    private final NukkitCloudNetSignsPlugin plugin;

    NukkitSignManagement(NukkitCloudNetSignsPlugin plugin) {
        instance = this;

        this.plugin = plugin;

        super.executeSearchingTask();
        super.executeStartingTask();
    }

    public static NukkitSignManagement getInstance() {
        return NukkitSignManagement.instance;
    }


    @Override
    protected void updateSignNext(Sign sign, SignLayout signLayout, ServiceInfoSnapshot serviceInfoSnapshot) {
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
        for (Sign sign : super.signs) {
            Location location = this.toLocation(sign.getWorldPosition());

            if (location == null || !(location.getLevel().getBlockEntity(location) instanceof BlockEntitySign)) {
                super.sendSignRemoveUpdate(sign);
            }
        }
    }

    @Override
    protected void runTaskLater(Runnable runnable, long delay) {
        Server.getInstance().getScheduler().scheduleDelayedTask(this.plugin, runnable, Math.toIntExact(delay));
    }

    private void updateSign(Sign sign, BlockEntitySign nukkitSign, SignLayout signLayout, ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(nukkitSign);
        Validate.checkNotNull(signLayout);

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
        Validate.checkNotNull(nukkitSign);

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

    public Location toLocation(SignPosition signPosition) {
        Validate.checkNotNull(signPosition);

        return Server.getInstance().getLevelByName(signPosition.getWorld()) != null ? new Location(
                signPosition.getX(),
                signPosition.getY(),
                signPosition.getZ(),
                Server.getInstance().getLevelByName(signPosition.getWorld())
        ) : null;
    }

    public NukkitCloudNetSignsPlugin getPlugin() {
        return this.plugin;
    }


}
