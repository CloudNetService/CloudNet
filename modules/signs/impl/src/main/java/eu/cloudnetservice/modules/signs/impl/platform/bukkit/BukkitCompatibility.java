/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.impl.platform.bukkit;

import com.google.common.base.Enums;
import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.utils.base.StringUtil;
import lombok.NonNull;
import org.bukkit.DyeColor;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Colorable;
import org.bukkit.material.Sign;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class BukkitCompatibility {

  private static final Class<?> WALL_SIGN_CLASS;

  private static final MethodAccessor<?> GET_BLOCK_DATA;
  private static final MethodAccessor<?> WALL_SIGN_GET_FACING;

  private static final MethodAccessor<?> SET_GLOWING;
  private static final MethodAccessor<?> SET_DYE_COLOR;

  static {
    // wall sign lookup
    var wallSignReflexion = Reflexion.find("org.bukkit.block.data.type.WallSign");
    WALL_SIGN_CLASS = wallSignReflexion.map(Reflexion::getWrappedClass).orElse(null);
    WALL_SIGN_GET_FACING = wallSignReflexion.flatMap(reflexion -> reflexion.findMethod("getFacing")).orElse(null);

    // get block data lookup
    GET_BLOCK_DATA = Reflexion.on(BlockState.class).findMethod("getBlockData").orElse(null);

    // glowing lookup
    SET_DYE_COLOR = Reflexion.on(Colorable.class).findMethod("setColor", DyeColor.class).orElse(null);
    SET_GLOWING = Reflexion.on(org.bukkit.block.Sign.class).findMethod("setGlowingText", boolean.class).orElse(null);
  }

  private BukkitCompatibility() {
    throw new UnsupportedOperationException();
  }

  public static @Nullable BlockFace facing(@NonNull BlockState blockState) {
    if (WALL_SIGN_CLASS != null && GET_BLOCK_DATA != null && WALL_SIGN_GET_FACING != null) {
      // modern bukkit lookup is possible
      var blockData = GET_BLOCK_DATA.invoke(blockState).getOrElse(null);
      if (WALL_SIGN_CLASS.isInstance(blockData)) {
        return WALL_SIGN_GET_FACING.<BlockFace>invoke(blockData).getOrElse(BlockFace.UP);
      }
    }
    // use legacy lookup
    var materialData = blockState.getData();
    if (materialData instanceof Sign sign) {
      return sign.isWallSign() ? sign.getFacing() : BlockFace.UP;
    }
    // unable to retrieve facing information
    return null;
  }

  public static void signGlowing(@NonNull org.bukkit.block.Sign sign, @NonNull SignLayout layout) {
    if (SET_GLOWING != null && SET_DYE_COLOR != null && layout.glowingColor() != null) {
      // try to find the defined dye color
      var color = Enums.getIfPresent(DyeColor.class, StringUtil.toUpper(layout.glowingColor())).orNull();
      if (color != null) {
        // enable the glowing of the sign
        SET_GLOWING.invoke(sign, Boolean.TRUE);
        SET_DYE_COLOR.invoke(sign, color);
      }
    }
  }
}
