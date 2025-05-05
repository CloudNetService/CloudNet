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

package eu.cloudnetservice.modules.signs.impl.platform.sponge;

import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignLayoutsHolder;
import eu.cloudnetservice.modules.signs.impl.InternalSignManagement;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSignManagement;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.WorldManager;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.plugin.PluginContainer;

@Singleton
@ProvidesFor(platform = "sponge", types = {
  PlatformSignManagement.class,
  InternalSignManagement.class,
  SignManagement.class
})
public class SpongeSignManagement extends PlatformSignManagement<ServerPlayer, ServerLocation, Component> {

  private final Game game;
  private final WorldManager worldManager;
  private final ServiceRegistry serviceRegistry;
  private final TaskExecutorService syncExecutor;
  private final EventManager eventManager;

  @Inject
  protected SpongeSignManagement(
    @NonNull Game game,
    @NonNull Server server,
    @NonNull WorldManager worldManager,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull EventManager spongeEventManager,
    @NonNull PluginContainer pluginContainer,
    @NonNull WrapperConfiguration wrapperConfig,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull @Named("sync") Scheduler syncScheduler,
    @NonNull @Named("taskScheduler") ScheduledExecutorService executorService,
    @NonNull eu.cloudnetservice.driver.event.EventManager eventManager
  ) {
    super(eventManager, runnable -> {
      // check if we're already on main
      if (server.onMainThread()) {
        runnable.run();
      } else {
        syncScheduler.submit(Task.builder().plugin(pluginContainer).execute(runnable).build());
      }
    }, wrapperConfig, serviceProvider, executorService);

    this.game = game;
    this.worldManager = worldManager;
    this.serviceRegistry = serviceRegistry;
    this.eventManager = spongeEventManager;
    this.syncExecutor = syncScheduler.executor(pluginContainer);
  }

  @Override
  protected int tps() {
    return 20;
  }

  @Override
  protected void startKnockbackTask() {
    this.syncExecutor.scheduleWithFixedDelay(() -> {
      var entry = this.applicableSignConfigurationEntry();
      if (entry != null) {
        var conf = entry.knockbackConfiguration();
        if (conf.validAndEnabled()) {
          var distance = conf.distance();
          // find all signs which need to knock back the player
          for (var sign : this.platformSigns.values()) {
            if (sign.needsUpdates() && sign.exists() && sign instanceof SpongePlatformSign spongeSign) {
              var location = spongeSign.signLocation();
              if (location != null) {
                var locationVec = location.position();
                for (Entity entity : location.world().nearbyEntities(locationVec, distance)) {
                  if (entity instanceof ServerPlayer player
                    && (conf.bypassPermission() == null || !player.hasPermission(conf.bypassPermission()))) {
                    var vector = entity.location()
                      .position()
                      .sub(locationVec)
                      .normalize()
                      .mul(conf.strength());
                    entity.velocity().set(new Vector3d(vector.x(), 0.2D, vector.z()));
                  }
                }
              }
            }
          }
        }
      }
    }, 0, 5 * 50, TimeUnit.MILLISECONDS);
  }

  @Override
  protected void tick(@NonNull Map<SignLayoutsHolder, Set<PlatformSign<ServerPlayer, Component>>> signsNeedingTicking) {
    this.mainThreadExecutor.execute(() -> {
      super.tick(signsNeedingTicking);
    });
  }

  @Override
  public @Nullable WorldPosition convertPosition(@NonNull ServerLocation location) {
    var entry = this.applicableSignConfigurationEntry();
    if (entry == null) {
      return null;
    }

    return new WorldPosition(
      location.x(),
      location.y(),
      location.z(),
      0,
      0,
      location.world().key().asString(),
      entry.targetGroup());
  }

  @Override
  protected @NonNull PlatformSign<ServerPlayer, Component> createPlatformSign(@NonNull Sign base) {
    return new SpongePlatformSign(base, this.game, this.eventManager, this.worldManager, this.serviceRegistry);
  }
}
