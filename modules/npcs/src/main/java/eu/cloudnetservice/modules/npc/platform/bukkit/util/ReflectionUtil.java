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

package eu.cloudnetservice.modules.npc.platform.bukkit.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReflectionUtil {

  private static final String NMS_PACKAGE;
  private static final String CRAFT_BUKKIT_PACKAGE;

  private static final Pattern PACKAGE_VERSION_PATTERN = Pattern
    .compile("^org\\.bukkit\\.craftbukkit\\.(\\w+)\\.CraftServer$");

  static {
    String nmsPackage = null;
    // get the craft bukkit package
    Matcher matcher = PACKAGE_VERSION_PATTERN.matcher(Bukkit.getServer().getClass().getName());
    if (!matcher.matches()) {
      throw new ExceptionInInitializerError("Unable to resolve craft bukkit package");
    }
    // assign the cb package
    CRAFT_BUKKIT_PACKAGE = String.format("org.bukkit.craftbukkit.%s.", matcher.group(1));
    try {
      // modern spigot servers are no longer using a versioned package name
      Class.forName("net.minecraft.server.MinecraftServer");
      nmsPackage = "net.minecraft.";
    } catch (ClassNotFoundException exception) {
      try {
        // should be fine - validate anyways
        Class.forName("net.minecraft.server." + matcher.group(1) + ".MinecraftServer");
        nmsPackage = "net.minecraft.server." + matcher.group(1) + '.';
      } catch (ClassNotFoundException ignored) {
      }
    }
    // check if we found the package
    if (nmsPackage == null) {
      throw new ExceptionInInitializerError("Unable to resolve nms package. THIS IS A BUG!");
    }
    // init fields
    NMS_PACKAGE = nmsPackage;
  }

  public static @NotNull Class<?> findNmsClass(String @NotNull ... names) {
    for (String name : names) {
      try {
        return Class.forName(NMS_PACKAGE + name);
      } catch (ClassNotFoundException ignored) {
      }
    }
    // unable to find the class
    throw new IllegalArgumentException("No nms class named " + String.join(", ", names) + " in " + NMS_PACKAGE);
  }

  public static @NotNull Class<?> findCraftBukkitClass(String @NotNull ... names) {
    for (String name : names) {
      try {
        return Class.forName(CRAFT_BUKKIT_PACKAGE + name);
      } catch (ClassNotFoundException ignored) {
      }
    }
    // unable to find the class
    throw new IllegalArgumentException(
      "No nms class named " + String.join(", ", names) + " in " + CRAFT_BUKKIT_PACKAGE);
  }

  public static @NotNull MethodHandle findMethod(
    @NotNull Class<?> clazz,
    @NotNull Class<?>[] pts,
    String @NotNull ... names
  ) {
    for (String name : names) {
      MethodHandle handle = findMethod(clazz, name, pts);
      if (handle != null) {
        return handle;
      }
    }
    throw new IllegalArgumentException("No method with name " + String.join(", ", names) + " in " + clazz);
  }

  public static @NotNull MethodHandle findConstructor(Class<?> clazz, Class<?> @NotNull ... argumentTypes) {
    try {
      return MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class, argumentTypes));
    } catch (NoSuchMethodException | IllegalAccessException exception) {
      throw new IllegalArgumentException("Unable to resolve constructor of class " + clazz, exception);
    }
  }

  public static @Nullable MethodHandle findMethod(
    @NotNull Class<?> clazz,
    @NotNull String name,
    Class<?> @NotNull ... pts
  ) {
    try {
      Method method = clazz.getDeclaredMethod(name, pts);
      return MethodHandles.publicLookup().unreflect(method);
    } catch (NoSuchMethodException | IllegalAccessException exception) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public static @Nullable <T> T getStaticFieldValue(@NotNull Class<?> origin, @NotNull String name) {
    try {
      Field field = origin.getDeclaredField(name);
      return (T) field.get(null);
    } catch (ReflectiveOperationException exception) {
      return null;
    }
  }
}
