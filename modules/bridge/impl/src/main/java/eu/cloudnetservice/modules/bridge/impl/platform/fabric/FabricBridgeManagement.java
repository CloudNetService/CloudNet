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

package eu.cloudnetservice.modules.bridge.impl.platform.fabric;

import com.mojang.authlib.properties.Property;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.platform.fabric.util.BridgedServer;
import eu.cloudnetservice.modules.bridge.impl.util.BridgeHostAndPortUtil;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.ServicePlayer;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.event.ServiceInfoPropertiesConfigureEvent;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import io.netty.util.AttributeKey;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.NonNull;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class FabricBridgeManagement extends PlatformBridgeManagement<ServerPlayer, NetworkPlayerServerInfo> {

  public static final boolean DISABLE_CLOUDNET_FORWARDING = Boolean.getBoolean("cloudnet.ipforward.disabled");

  public static final AttributeKey<UUID> PLAYER_FORWARDED_UUID_KEY =
    AttributeKey.newInstance("cloudnet_bridge$forwardedUniqueId");
  public static final AttributeKey<Void> PLAYER_INTENTION_PACKET_SEEN_KEY =
    AttributeKey.newInstance("cloudnet_bridge$intentionPacketSeen");
  public static final AttributeKey<Property[]> PLAYER_PROFILE_PROPERTIES_KEY =
    AttributeKey.newInstance("cloudnet_bridge$forwardedProperties");

  private final BridgedServer server;
  private final PlayerExecutor directGlobalExecutor;

  public FabricBridgeManagement(
    @NonNull BridgedServer server,
    @NonNull RPCFactory rpcFactory,
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull BridgeServiceHelper serviceHelper,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull WrapperConfiguration wrapperConfiguration
  ) {
    super(
      rpcFactory,
      eventManager,
      networkClient,
      taskProvider,
      serviceHelper,
      serviceInfoHolder,
      serviceProvider,
      wrapperConfiguration);
    // field init
    this.server = server;
    this.directGlobalExecutor = new FabricDirectPlayerExecutor(
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      server::cloudnet_bridge$players);
    // init the bridge properties
    serviceHelper.motd().set(server.cloudnet_bridge$motd());
    serviceHelper.maxPlayers().set(server.cloudnet_bridge$maxPlayers());
  }

  @Override
  public void registerServices(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlayerManager.class, "PlayerManager", this.playerManager);
  }

  @Override
  public @NonNull ServicePlayer wrapPlayer(@NonNull ServerPlayer player) {
    return new ServicePlayer(player.getUUID(), player.getGameProfile().getName());
  }

  @Override
  public @NonNull NetworkPlayerServerInfo createPlayerInformation(@NonNull ServerPlayer player) {
    return new NetworkPlayerServerInfo(
      player.getUUID(),
      player.getGameProfile().getName(),
      null,
      BridgeHostAndPortUtil.fromSocketAddress(player.connection.getRemoteAddress()),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NonNull BiFunction<ServerPlayer, String, Boolean> permissionFunction() {
    return (player, perm) -> true;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NonNull ServerPlayer player) {
    return this.isOnAnyFallbackInstance(this.ownNetworkServiceInfo.serverName(), null, perm -> true);
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull ServerPlayer player) {
    return this.fallback(player, this.ownNetworkServiceInfo.serverName());
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(
    @NonNull ServerPlayer player,
    @Nullable String currServer
  ) {
    return this.fallback(player.getUUID(), currServer, null, perm -> true);
  }

  @Override
  public void handleFallbackConnectionSuccess(@NonNull ServerPlayer player) {
    this.handleFallbackConnectionSuccess(player.getUUID());
  }

  @Override
  public void removeFallbackProfile(@NonNull ServerPlayer player) {
    this.removeFallbackProfile(player.getUUID());
  }

  @Override
  public @NonNull PlayerExecutor directPlayerExecutor(@NonNull UUID uniqueId) {
    return uniqueId.equals(PlayerExecutor.GLOBAL_UNIQUE_ID)
      ? this.directGlobalExecutor
      : new FabricDirectPlayerExecutor(
        uniqueId,
        () -> Collections.singleton(this.server.cloudnet_bridge$player(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoPropertiesConfigureEvent configureEvent) {
    super.appendServiceInformation(configureEvent);

    configureEvent.propertyHolder().append("Online-Count", this.server.cloudnet_bridge$playerCount());
    configureEvent.propertyHolder().append("Version", SharedConstants.getCurrentVersion().getName());
    // players
    configureEvent.propertyHolder().append("Players", this.server.cloudnet_bridge$players().stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
