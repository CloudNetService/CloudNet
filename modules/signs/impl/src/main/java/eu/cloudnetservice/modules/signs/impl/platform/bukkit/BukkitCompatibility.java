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
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.utils.base.StringUtil;
import io.vavr.CheckedFunction2;
import io.vavr.CheckedFunction3;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import lombok.NonNull;
import org.bukkit.DyeColor;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Colorable;
import org.bukkit.material.Sign;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApiStatus.Internal
public final class BukkitCompatibility {

  private static final Logger LOGGER = LoggerFactory.getLogger(BukkitCompatibility.class);

  private static final Class<?> DIRECTIONAL_CLASS;
  private static final Class<?> HANGING_SIGN_CLASS;
  private static final Class<?> WALL_HANGING_SIGN_CLASS;

  // (BlockState):BlockData converted as (BlockState):Object
  private static final MethodHandle GET_BLOCK_DATA;
  // (BlockData):BlockFace (only works for directional block data) converted as (Object):BlockFace
  private static final MethodHandle GET_FACING_FROM_BLOCK_DATA;

  // enables the glowing effect on the given target sign
  private static final CheckedFunction2<org.bukkit.block.Sign, Boolean, Void> SIGN_SET_GLOWING;
  // sets the text color of the given target sign
  private static final CheckedFunction2<org.bukkit.block.Sign, DyeColor, Void> SIGN_SET_TEXT_COLOR;
  // sets the text on the sign in the given line to the given text
  private static final CheckedFunction3<org.bukkit.block.Sign, Integer, String, Void> SIGN_SET_TEXT;

  // maximum count of warnings that should be logged to not flood the console
  private static final int MAX_LOGGED_WARNINGS = 15;
  private static int LOGGED_WARNING_COUNT = 0;

  static {
    var lookup = MethodHandles.publicLookup(); // everything should be public api

    // resolve required classes [Directional was introduced in 1.13-pre7, HangingSign in 1.20]
    DIRECTIONAL_CLASS = Reflexion.find("org.bukkit.block.data.Directional")
      .map(Reflexion::getWrappedClass)
      .orElse(null);
    HANGING_SIGN_CLASS = Reflexion.find("org.bukkit.block.data.type.HangingSign")
      .map(Reflexion::getWrappedClass)
      .orElse(null);
    WALL_HANGING_SIGN_CLASS = Reflexion.find("org.bukkit.block.data.type.WallHangingSign")
      .map(Reflexion::getWrappedClass)
      .orElse(null);

    // resolve a method handle to get the BlockData of a BlockState (introduced in 1.13-pre7)
    MethodHandle blockStateGetBlockData = null;
    try {
      var blockDataClass = Class.forName("org.bukkit.block.data.BlockData");
      blockStateGetBlockData = lookup
        .findVirtual(BlockState.class, "getBlockData", MethodType.methodType(blockDataClass))
        .asType(MethodType.methodType(Object.class, BlockState.class));
      LOGGER.debug("org.bukkit.block.BlockState.getBlockData(): available");
    } catch (Exception ex) {
      LOGGER.debug("org.bukkit.block.BlockState.getBlockData(): unavailable ({})", ex.getMessage());
    }

    // resolve the method handle to get the block face of a directional BlockData
    MethodHandle blockDataGetFacing = null;
    if (DIRECTIONAL_CLASS != null) {
      try {
        var directionalGetFacing = lookup.findVirtual(
          DIRECTIONAL_CLASS,
          "getFacing",
          MethodType.methodType(BlockFace.class));
        blockDataGetFacing = MethodHandles.explicitCastArguments(
          directionalGetFacing,
          MethodType.methodType(BlockFace.class, Object.class));
        LOGGER.debug("org.bukkit.block.data.Directional.getFacing(): available");
      } catch (Exception ex) {
        LOGGER.debug("org.bukkit.block.data.Directional.getFacing(): unavailable ({})", ex.getMessage());
      }
    }

    // resolves method handles to get the front and back side of a sign (available since 1.20)
    Class<?> signSideClass = null;
    MethodHandle getFrontSide = null;
    MethodHandle getBackSide = null;
    try {
      signSideClass = Class.forName("org.bukkit.block.sign.SignSide");
      var signSidesClass = Class.forName("org.bukkit.block.sign.Side");
      var getSignSide = lookup.findVirtual(
        org.bukkit.block.Sign.class,
        "getSide",
        MethodType.methodType(signSideClass, signSidesClass));

      // insert one side argument into the handle to prevent the necessity to pass it each time
      var signSides = signSidesClass.getEnumConstants();
      getFrontSide = MethodHandles.insertArguments(getSignSide, 1, signSides[0]);
      getBackSide = MethodHandles.insertArguments(getSignSide, 1, signSides[1]);
      LOGGER.debug("org.bukkit.block.sign.SignSide: available");
    } catch (Exception exception) {
      LOGGER.debug("org.bukkit.block.sign.SignSide: unavailable ({})", exception.getMessage());
    }

    // resolves the function to enable glowing on the given target sign (since 1.17)
    CheckedFunction2<org.bukkit.block.Sign, Boolean, Void> setSignGlowing = null;
    try {
      if (signSideClass != null && getFrontSide != null && getBackSide != null) {
        // modern way since 1.20: SignSide.setGlowingText(boolean)
        var setGlowingText = lookup.findVirtual(
          signSideClass,
          "setGlowingText",
          MethodType.methodType(void.class, boolean.class));
        var setGlowingFront = MethodHandles.collectArguments(setGlowingText, 0, getFrontSide);
        var setGlowingBack = MethodHandles.collectArguments(setGlowingText, 0, getBackSide);
        setSignGlowing = (sign, glowing) -> {
          setGlowingFront.invokeExact(sign, (boolean) glowing);
          setGlowingBack.invokeExact(sign, (boolean) glowing);
          return null;
        };
      } else {
        // legacy way for 1.17 to 1.20: Sign.setGlowingText(boolean)
        var setGlowingText = lookup.findVirtual(
          org.bukkit.block.Sign.class,
          "setGlowingText",
          MethodType.methodType(void.class, boolean.class));
        setSignGlowing = (sign, glowing) -> {
          setGlowingText.invokeExact(sign, (boolean) glowing);
          return null;
        };
      }
    } catch (Exception exception) {
      LOGGER.debug("org.bukkit.block.Sign.setGlowingText(): unavailable ({})", exception.getMessage());
    }

    // resolves the function to the set text color on the given target to the given target color (since 1.14)
    CheckedFunction2<org.bukkit.block.Sign, DyeColor, Void> setSignTextColor = null;
    try {
      if (signSideClass != null && getFrontSide != null && getBackSide != null) {
        // modern way since 1.20: SignSide.setColor(DyeColor)
        var setColor = lookup.findVirtual(
          signSideClass,
          "setColor",
          MethodType.methodType(void.class, DyeColor.class));
        var setColorFront = MethodHandles.collectArguments(setColor, 0, getFrontSide);
        var setColorBack = MethodHandles.collectArguments(setColor, 0, getBackSide);
        setSignTextColor = (sign, dyeColor) -> {
          setColorFront.invokeExact(sign, dyeColor);
          setColorBack.invokeExact(sign, dyeColor);
          return null;
        };
      } else {
        // legacy way for 1.14 to 1.20: Sign.setColor(DyeColor)
        setSignTextColor = (sign, dyeColor) -> {
          if (sign instanceof Colorable colorable) {
            colorable.setColor(dyeColor);
          }
          return null;
        };
      }
    } catch (Exception exception) {
      LOGGER.debug("org.bukkit.block.Sign.setColor: unavailable ({})", exception.getMessage());
    }

    // resolves the function to set a line on the given target sign to the given text
    CheckedFunction3<org.bukkit.block.Sign, Integer, String, Void> setSignText = null;
    try {
      if (signSideClass != null && getFrontSide != null && getBackSide != null) {
        // modern way since 1.20: SignSide.setLine(index, text)
        var setLine = lookup.findVirtual(
          signSideClass,
          "setLine",
          MethodType.methodType(void.class, int.class, String.class));
        var setLineFront = MethodHandles.collectArguments(setLine, 0, getFrontSide);
        var setLineBack = MethodHandles.collectArguments(setLine, 0, getBackSide);
        setSignText = (sign, line, text) -> {
          setLineFront.invokeExact(sign, (int) line, text);
          setLineBack.invokeExact(sign, (int) line, text);
          return null;
        };
      } else {
        // legacy way before 1.20
        setSignText = (sign, line, text) -> {
          sign.setLine(line, text);
          return null;
        };
      }
    } catch (Exception exception) {
      LOGGER.debug("org.bukkit.block.Sign.setLine(): unavailable ({})", exception.getMessage());
    }

    GET_BLOCK_DATA = blockStateGetBlockData;
    GET_FACING_FROM_BLOCK_DATA = blockDataGetFacing;
    SIGN_SET_GLOWING = setSignGlowing;
    SIGN_SET_TEXT_COLOR = setSignTextColor;
    SIGN_SET_TEXT = setSignText;
  }

  private BukkitCompatibility() {
    throw new UnsupportedOperationException();
  }

  /**
   * Logs an exception message into the {@code WARN} level of the logger. Warnings are only logged if the maximum
   * warning log count hasn't been reached yet (to prevent the console from being flooded).
   *
   * @param baseMessage      the base warning message to log.
   * @param exceptionMessage the message of the caught exception.
   * @throws NullPointerException if the given base message is null.
   */
  private static void logExceptionMessage(@NonNull String baseMessage, @Nullable String exceptionMessage) {
    if (MAX_LOGGED_WARNINGS > LOGGED_WARNING_COUNT) {
      LOGGED_WARNING_COUNT++;
      LOGGER.warn(baseMessage, exceptionMessage);
    }
  }

  /**
   * Gets the facing of the given target sign block state. This usually returns:
   * <ol>
   *   <li>any block face for signs attached to a wall.
   *   <li>{@code DOWN} for signs hanging from the ceiling.
   *   <li>{@code UP} for signs that are standing.
   *   <li>{@code null} for signs hanging from a wall, these signs are not attached to anything.
   * </ol>
   *
   * @param blockState the block state of the sign to get the facing of.
   * @return the facing of the given sign or null if it cannot be resolved or doesn't exist.
   * @throws NullPointerException if the given sign block state is null.
   */
  public static @Nullable BlockFace facing(@NonNull BlockState blockState) {
    // modern lookup
    if (GET_BLOCK_DATA != null && GET_FACING_FROM_BLOCK_DATA != null) {
      try {
        var blockData = GET_BLOCK_DATA.invokeExact(blockState);
        if (WALL_HANGING_SIGN_CLASS != null && WALL_HANGING_SIGN_CLASS.isInstance(blockData)) {
          // the sign is hanging from a wall, these signs are actually not attached to anything (it can even float)
          return null;
        } else if (DIRECTIONAL_CLASS.isInstance(blockData)) {
          // the sign is directional (e.g., a wall sign)
          return (BlockFace) GET_FACING_FROM_BLOCK_DATA.invokeExact(blockData);
        } else if (HANGING_SIGN_CLASS != null && HANGING_SIGN_CLASS.isInstance(blockData)) {
          // the sign is hanging from the ceiling (so it's facing down)
          return BlockFace.DOWN;
        } else {
          // the sign is a standing sign (so it's facing up)
          return BlockFace.UP;
        }
      } catch (Throwable throwable) {
        logExceptionMessage("Unable to get sign facing using modern lookup: {}", throwable.getMessage());
      }
    }

    // legacy lookup
    var materialData = blockState.getData();
    if (materialData instanceof Sign sign) {
      return sign.isWallSign() ? sign.getFacing() : BlockFace.UP;
    }

    return null;
  }

  /**
   * Applies the text color defined in the given layout to the given sign. Falls back to black if not defined.
   *
   * @param sign   the sign to apply the text color to.
   * @param layout the layout to get the text color from.
   * @throws NullPointerException if the given sign or layout is null.
   */
  public static void signTextColor(@NonNull org.bukkit.block.Sign sign, @NonNull SignLayout layout) {
    if (SIGN_SET_TEXT_COLOR != null) {
      try {
        var textColor = layout.textColor();
        var dyeColor = switch (textColor) {
          case String string -> Enums.getIfPresent(DyeColor.class, StringUtil.toUpper(string)).or(DyeColor.BLACK);
          case null -> DyeColor.BLACK;
        };
        SIGN_SET_TEXT_COLOR.apply(sign, dyeColor);
      } catch (Throwable throwable) {
        logExceptionMessage("Unable to set sign text color: {}", throwable.getMessage());
      }
    }
  }

  /**
   * Enables the glowing effect for the given signs if the given layout has that feature enabled.
   *
   * @param sign   the sign to enable the glowing effect for.
   * @param layout the layout to resolve the glowing effect from.
   * @throws NullPointerException if the given target sign or sign layout is null.
   */
  public static void signGlowing(@NonNull org.bukkit.block.Sign sign, @NonNull SignLayout layout) {
    if (SIGN_SET_GLOWING != null) {
      try {
        SIGN_SET_GLOWING.apply(sign, layout.textGlowing());
      } catch (Throwable throwable) {
        logExceptionMessage("Unable to set sign glowing: {}", throwable.getMessage());
      }
    }
  }

  /**
   * Sets a line of the given sign (on both sides if that is available).
   *
   * @param sign the sign to set the line on.
   * @param line the index of the line to set the text on.
   * @param text the text to set on the sign at the specified line.
   * @throws NullPointerException if the given sign or line index is null.
   */
  public static void signLine(@NonNull org.bukkit.block.Sign sign, @NonNull Integer line, @Nullable String text) {
    if (SIGN_SET_TEXT != null) {
      try {
        SIGN_SET_TEXT.apply(sign, line, text);
      } catch (Throwable throwable) {
        logExceptionMessage("Unable to set sign line: {}", throwable.getMessage());
      }
    }
  }
}
