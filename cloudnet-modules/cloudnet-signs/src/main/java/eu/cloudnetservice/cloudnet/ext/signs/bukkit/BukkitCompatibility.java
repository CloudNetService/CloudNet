package eu.cloudnetservice.cloudnet.ext.signs.bukkit;

import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sign;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@ApiStatus.Internal
public final class BukkitCompatibility {

    private static final Class<?> WALL_SIGN_CLASS;

    private static final MethodHandle GET_BLOCK_DATA;
    private static final MethodHandle WALL_SIGN_GET_FACING;

    static {
        Class<?> wallSignClass;

        MethodHandle getBlockData;
        MethodHandle getFacing;

        try {
            wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");
            Class<?> blockDataClass = Class.forName("org.bukkit.block.data.BlockData");

            getBlockData = MethodHandles.publicLookup().findVirtual(BlockState.class, "getBlockData",
                    MethodType.methodType(blockDataClass));
            getFacing = MethodHandles.publicLookup().findVirtual(wallSignClass, "getFacing",
                    MethodType.methodType(BlockFace.class));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            wallSignClass = null;
            getBlockData = null;
            getFacing = null;
        }

        WALL_SIGN_CLASS = wallSignClass;
        GET_BLOCK_DATA = getBlockData;
        WALL_SIGN_GET_FACING = getFacing;
    }

    private BukkitCompatibility() {
        throw new UnsupportedOperationException();
    }

    public static @Nullable BlockFace getFacing(@NotNull BlockState blockState) {
        if (WALL_SIGN_CLASS != null && GET_BLOCK_DATA != null && WALL_SIGN_GET_FACING != null) {
            // modern bukkit lookup is possible
            try {
                Object blockData = GET_BLOCK_DATA.invoke(blockState);
                if (WALL_SIGN_CLASS.isInstance(blockData)) {
                    return (BlockFace) WALL_SIGN_GET_FACING.invoke(blockData);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            return BlockFace.UP;
        }
        // use legacy lookup
        MaterialData materialData = blockState.getData();
        if (materialData instanceof Sign) {
            Sign sign = (Sign) materialData;
            return sign.isWallSign() ? sign.getFacing() : BlockFace.UP;
        }
        // unable to retrieve facing information
        return null;
    }
}
