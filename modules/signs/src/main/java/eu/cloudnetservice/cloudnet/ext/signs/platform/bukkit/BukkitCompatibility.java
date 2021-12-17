/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.cloudnet.ext.signs.platform.bukkit;

import com.google.common.base.Enums;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.bukkit.DyeColor;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Colorable;
import org.bukkit.material.Sign;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Internal
public final class BukkitCompatibility {

  private static final Logger LOGGER = LogManager.getLogger(BukkitCompatibility.class);

  private static final Class<?> WALL_SIGN_CLASS;

  private static final MethodHandle GET_BLOCK_DATA;
  private static final MethodHandle WALL_SIGN_GET_FACING;

  private static final MethodHandle SET_GLOWING;
  private static final MethodHandle SET_DYE_COLOR;

  static {
    // facing lookup
    Class<?> wallSignClass;

    MethodHandle getBlockData;
    MethodHandle getFacing;

    try {
      wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");
      var blockDataClass = Class.forName("org.bukkit.block.data.BlockData");

      getBlockData = MethodHandles.publicLookup().findVirtual(BlockState.class, "getBlockData",
        MethodType.methodType(blockDataClass));
      getFacing = MethodHandles.publicLookup().findVirtual(wallSignClass, "getFacing",
        MethodType.methodType(BlockFace.class));
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
      wallSignClass = null;
      getBlockData = null;
      getFacing = null;
    }

    // glowing lookup
    MethodHandle setGlowing;
    MethodHandle setDyeColor;

    try {
      setGlowing = MethodHandles.publicLookup().findVirtual(org.bukkit.block.Sign.class, "setGlowingText",
        MethodType.methodType(void.class, boolean.class));
      setDyeColor = MethodHandles.publicLookup().findVirtual(Colorable.class, "setColor",
        MethodType.methodType(void.class, DyeColor.class));
    } catch (NoSuchMethodException | IllegalAccessException exception) {
      // not available (only 1.17+)
      setGlowing = null;
      setDyeColor = null;
    }

    WALL_SIGN_CLASS = wallSignClass;
    GET_BLOCK_DATA = getBlockData;
    WALL_SIGN_GET_FACING = getFacing;

    SET_GLOWING = setGlowing;
    SET_DYE_COLOR = setDyeColor;
  }

  private BukkitCompatibility() {
    throw new UnsupportedOperationException();
  }

  public static @Nullable BlockFace getFacing(@NotNull BlockState blockState) {
    if (WALL_SIGN_CLASS != null && GET_BLOCK_DATA != null && WALL_SIGN_GET_FACING != null) {
      // modern bukkit lookup is possible
      try {
        var blockData = GET_BLOCK_DATA.invoke(blockState);
        if (WALL_SIGN_CLASS.isInstance(blockData)) {
          return (BlockFace) WALL_SIGN_GET_FACING.invoke(blockData);
        }
      } catch (Throwable throwable) {
        LOGGER.severe("Exception while checking the BlockFace", throwable);
      }
      return BlockFace.UP;
    }
    // use legacy lookup
    var materialData = blockState.getData();
    if (materialData instanceof Sign sign) {
      return sign.isWallSign() ? sign.getFacing() : BlockFace.UP;
    }
    // unable to retrieve facing information
    return null;
  }

  public static void setSignGlowing(@NotNull org.bukkit.block.Sign sign, @NotNull SignLayout layout) {
    if (SET_GLOWING != null && SET_DYE_COLOR != null && layout.glowingColor() != null) {
      // try to find the defined dye color
      var color = Enums.getIfPresent(DyeColor.class, layout.glowingColor().toUpperCase()).orNull();
      if (color != null) {
        try {
          // enable the glowing of the sign
          SET_GLOWING.invoke(sign, Boolean.TRUE);
          SET_DYE_COLOR.invoke(sign, color);
        } catch (Throwable throwable) {
          LOGGER.severe("Exception while invoking glowing signs", throwable);
        }
      }
    }
  }
}
