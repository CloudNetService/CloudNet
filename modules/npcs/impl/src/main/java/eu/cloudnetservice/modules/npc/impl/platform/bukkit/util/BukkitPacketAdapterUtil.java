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
import java.lang.invoke.MethodHandle;
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

public final class BukkitPacketAdapterUtil {

  private static final String NPC_PACKET_ADAPTER = System.getProperty("cloudnet.npcs.packet-adapter");

  private static final MethodHandle SERVER_GET_BUKKIT_VERSION;

  static {
    MethodHandle handle = null;
    try {
      handle = MethodHandles.publicLookup().findVirtual(Server.class, "getBukkitVersion", MethodType.methodType(String.class));
    } catch (Throwable _) {
    }
    SERVER_GET_BUKKIT_VERSION = handle;
  }

  private static final Supplier<PlatformPacketAdapter<World, Player, ItemStack, Plugin>> RESOLVED = StableValue.supplier(
    BukkitPacketAdapterUtil::resolveAdapter);

  private BukkitPacketAdapterUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull PlatformPacketAdapter<World, Player, ItemStack, Plugin> resolve() {
    return RESOLVED.get();
  }

  private static @NonNull PlatformPacketAdapter<World, Player, ItemStack, Plugin> resolveAdapter() {
    if (NPC_PACKET_ADAPTER == null) {
      return evaluateAdapter();
    }

    return switch (StringUtil.toLower(NPC_PACKET_ADAPTER)) {
      case "protocollib" -> BukkitProtocolAdapter.protocolLib();
      case "packetevents" -> BukkitProtocolAdapter.packetEvents();
      default -> evaluateAdapter();
    };
  }

  private static @NonNull PlatformPacketAdapter<World, Player, ItemStack, Plugin> evaluateAdapter() {
    try {
      var bukkitVersion = resolveMinecraftVersion();
      var versionStart = bukkitVersion.indexOf('-');
      var parsedVersion = PEVersion.fromString(versionStart >= 0 ? bukkitVersion.substring(0, versionStart) : bukkitVersion);
      var latestPEVersion = PEVersion.fromString(ServerVersion.getLatest().getReleaseName());
      if (parsedVersion.isNewerThan(latestPEVersion)) {
        return BukkitProtocolAdapter.protocolLib();
      }

      return BukkitProtocolAdapter.packetEvents();
    } catch (Throwable _) {
      return BukkitProtocolAdapter.protocolLib();
    }
  }

  private static @NonNull String resolveMinecraftVersion() {
    try {
      if (SERVER_GET_BUKKIT_VERSION != null) {
        return (String) SERVER_GET_BUKKIT_VERSION.invoke(Bukkit.getServer());
      }
    } catch (Throwable _) {
    }
    return Bukkit.getBukkitVersion();
  }
}


