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

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import net.fabricmc.loader.api.FabricLoader;

@Singleton
@PlatformPlugin(
  platform = "fabric",
  name = "CloudNet-Bridge",
  version = "@version@",
  dependencies = {
    @Dependency(name = "fabricloader", version = ">=0.17.0"),
    @Dependency(name = "minecraft", version = "*"),
    @Dependency(name = "java", version = "25")
  },
  authors = "CloudNetService",
  pluginFileNames = "fabric.mod.json.temp"
)
public final class FabricBridgeInitializer implements PlatformEntrypoint {

  private final EventManager eventManager;
  private final ServiceInfoHolder serviceInfoHolder;

  @Inject
  public FabricBridgeInitializer(@NonNull EventManager eventManager, @NonNull ServiceInfoHolder serviceInfoHolder) {
    this.eventManager = eventManager;
    this.serviceInfoHolder = serviceInfoHolder;
  }

  @Override
  public void onLoad() {
    // check if a version bridge was loaded. if that is not the case, we need to set the bridge as
    // online manually, to allow for players to connect. this is done after a small delay to allow
    // the server to boot before players try to connect to it
    var noVersionBrideLoaded = FabricLoader.getInstance().getModContainer("cloudnet_version_bridge").isEmpty();
    if (noVersionBrideLoaded) {
      var defaultRegistrationDelay = TimeUnit.SECONDS.toMillis(10);
      var configuredDelay = Long.getLong("cloudnet.fabric.fallback-registration-delay-ms", defaultRegistrationDelay);
      var registrationDelay = Math.max(0, configuredDelay);
      FabricLoaderLogger.info("No version bridge found, triggering manual registration in %s ms", registrationDelay);

      var delayedExecutor = CompletableFuture.delayedExecutor(registrationDelay, TimeUnit.MILLISECONDS);
      delayedExecutor.execute(() -> {
        this.eventManager.registerListener(FabricFallbackEventListener.class);
        this.serviceInfoHolder.publishServiceInfoUpdate();
        FabricLoaderLogger.debug("Manual service registration complete");
      });
    }
  }
}
