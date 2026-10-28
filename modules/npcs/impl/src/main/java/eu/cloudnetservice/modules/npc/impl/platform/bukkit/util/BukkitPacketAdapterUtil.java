/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.npc.impl.platform.bukkit.util;

import com.github.juliarn.npclib.api.protocol.PlatformPacketAdapter;
import com.github.juliarn.npclib.bukkit.protocol.BukkitProtocolAdapter;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.util.PEVersion;
import eu.cloudnetservice.utils.base.StringUtil;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BukkitPacketAdapterUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(BukkitPacketAdapterUtil.class);

  private static final String SYS_PROP_PACKET_ADAPTER = "cloudnet.npcs.packet-adapter";
  private static final Supplier<PlatformPacketAdapter<World, Player, ItemStack, Plugin>> PACKET_ADAPTER_HOLDER =
    StableValue.supplier(BukkitPacketAdapterUtil::resolveAdapter);

  private BukkitPacketAdapterUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Resolves the packet adapter to use, either using a specific override or the Minecraft version of the server. This
   * method lazily initializes the version and always returns the same value after the first invocation.
   *
   * @return the npc packet adapter to use.
   */
  public static @NonNull PlatformPacketAdapter<World, Player, ItemStack, Plugin> resolve() {
    return PACKET_ADAPTER_HOLDER.get();
  }

  /**
   * Resolves the packet adapter to use, either using a specific override or the Minecraft version of the server.
   *
   * @return the npc packet adapter to use.
   */
  private static @NonNull PlatformPacketAdapter<World, Player, ItemStack, Plugin> resolveAdapter() {
    var overriddenAdapterName = System.getProperty(SYS_PROP_PACKET_ADAPTER);
    return switch (StringUtil.toLower(overriddenAdapterName)) {
      case "protocollib" -> BukkitProtocolAdapter.protocolLib();
      case "packetevents" -> BukkitProtocolAdapter.packetEvents();
      case null, default -> evaluateAdapter();
    };
  }

  /**
   * Evaluates the packet adapter to use based on the current Minecraft version running on the server. If no reliable
   * guess can be made, this method returns the {@code ProtocolLib} packet adapter implementation.
   *
   * @return the packet adapter to use based on the current Minecraft version running on the server.
   */
  private static @NonNull PlatformPacketAdapter<World, Player, ItemStack, Plugin> evaluateAdapter() {
    try {
      var minecraftVersion = resolveMinecraftVersion();
      var parsedMinecraftVersion = PEVersion.fromString(minecraftVersion);
      var latestPeSupportedMinecraftVersion = PEVersion.fromString(ServerVersion.getLatest().getReleaseName());
      var packetEventsNotSupported = parsedMinecraftVersion.isNewerThan(latestPeSupportedMinecraftVersion);
      return packetEventsNotSupported ? BukkitProtocolAdapter.protocolLib() : BukkitProtocolAdapter.packetEvents();
    } catch (Exception exception) {
      LOGGER.warn(
        "Failed to reliably determine supported packet adapter ({}: {}), falling back to ProtocolLib",
        exception.getClass().getSimpleName(), exception.getMessage());
      return BukkitProtocolAdapter.protocolLib();
    }
  }

  /**
   * Resolves the Minecraft version currently running on the server.
   *
   * @return the Minecraft version currently running on the server.
   * @throws RuntimeException if no reliable guess can be made about the running version.
   */
  private static @NonNull String resolveMinecraftVersion() {
    try {
      var lookup = MethodHandles.publicLookup();
      var getMinecraftVersion = lookup.findVirtual(
        Server.class,
        "getMinecraftVersion",
        MethodType.methodType(String.class));
      return (String) getMinecraftVersion.invokeExact(Bukkit.getServer());
    } catch (Throwable _) {
      var bukkitVersion = Bukkit.getBukkitVersion();
      var maybeMinecraftVersion = bukkitVersion.split("-", 2)[0];
      if (maybeMinecraftVersion.indexOf('.') == -1) {
        var msg = String.format(
          "Failed to determine Minecraft version from '%s'. Use the system property '%s' to set a packet adapter",
          bukkitVersion, SYS_PROP_PACKET_ADAPTER);
        throw new RuntimeException(msg);
      }

      return maybeMinecraftVersion;
    }
  }
}
