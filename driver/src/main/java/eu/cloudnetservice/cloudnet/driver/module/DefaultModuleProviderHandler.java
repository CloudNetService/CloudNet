/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.module;

import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.event.Event;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePostInstallDependencyEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePostLoadEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePostReloadEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePostStartEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePostStopEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePostUnloadEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePreInstallDependencyEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePreLoadEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePreReloadEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePreStartEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePreStopEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.module.ModulePreUnloadEvent;
import lombok.NonNull;

public class DefaultModuleProviderHandler implements ModuleProviderHandler {

  private static final Logger LOGGER = LogManager.logger(DefaultModuleProviderHandler.class);

  @Override
  public boolean handlePreModuleLoad(@NonNull ModuleWrapper moduleWrapper) {
    var cancelled = this.callEvent(new ModulePreLoadEvent(this.moduleProvider(), moduleWrapper)).cancelled();
    if (!cancelled) {
      LOGGER.info(I18n.trans("cloudnet-pre-load-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
    }
    return !cancelled;
  }

  @Override
  public void handlePostModuleLoad(@NonNull ModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostLoadEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.fine(I18n.trans("cloudnet-post-load-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }

  @Override
  public boolean handlePreModuleStart(@NonNull ModuleWrapper moduleWrapper) {
    var cancelled = this.callEvent(new ModulePreStartEvent(this.moduleProvider(), moduleWrapper)).cancelled();
    if (!cancelled) {
      LOGGER.info(I18n.trans("cloudnet-pre-start-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
      CloudNetDriver.instance().eventManager().registerListener(moduleWrapper.module());
    }
    return !cancelled;
  }

  @Override
  public void handlePostModuleStart(@NonNull ModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostStartEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.fine(I18n.trans("cloudnet-post-start-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }

  @Override
  public boolean handlePreModuleReload(@NonNull ModuleWrapper moduleWrapper) {
    var cancelled = this.callEvent(new ModulePreReloadEvent(this.moduleProvider(), moduleWrapper)).cancelled();
    if (!cancelled) {
      LOGGER.info(I18n.trans("cloudnet-pre-reload-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
    }
    return !cancelled;
  }

  @Override
  public void handlePostModuleReload(@NonNull ModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostReloadEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.fine(I18n.trans("cloudnet-post-reload-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }


  @Override
  public boolean handlePreModuleStop(@NonNull ModuleWrapper moduleWrapper) {
    var cancelled = this.callEvent(new ModulePreStopEvent(this.moduleProvider(), moduleWrapper)).cancelled();
    if (!cancelled) {
      LOGGER.info(I18n.trans("cloudnet-pre-stop-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
    }
    return !cancelled;
  }

  @Override
  public void handlePostModuleStop(@NonNull ModuleWrapper moduleWrapper) {
    CloudNetDriver.instance().servicesRegistry().unregisterAll(moduleWrapper.classLoader());
    CloudNetDriver.instance().eventManager().unregisterListeners(moduleWrapper.classLoader());

    this.callEvent(new ModulePostStopEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.fine(I18n.trans("cloudnet-post-stop-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }

  @Override
  public void handlePreModuleUnload(@NonNull ModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePreUnloadEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.info(I18n.trans("cloudnet-pre-unload-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }

  @Override
  public void handlePostModuleUnload(@NonNull ModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostUnloadEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.fine(I18n.trans("cloudnet-post-unload-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }

  @Override
  public void handlePreInstallDependency(
    @NonNull ModuleConfiguration configuration,
    @NonNull ModuleDependency dependency
  ) {
    this.callEvent(new ModulePreInstallDependencyEvent(this.moduleProvider(), configuration, dependency));
    LOGGER.fine(I18n.trans("cloudnet-pre-install-dependency-module",
      this.moduleArguments(configuration),
      dependency.group(),
      dependency.name(),
      dependency.version()));
  }

  @Override
  public void handlePostInstallDependency(
    @NonNull ModuleConfiguration configuration,
    @NonNull ModuleDependency dependency
  ) {
    this.callEvent(new ModulePostInstallDependencyEvent(this.moduleProvider(), configuration, dependency));
    LOGGER.fine(I18n.trans("cloudnet-post-install-dependency-module",
      this.moduleArguments(configuration),
      dependency.group(),
      dependency.name(),
      dependency.version()));
  }


  protected ModuleProvider moduleProvider() {
    return CloudNetDriver.instance().moduleProvider();
  }

  protected @NonNull <T extends Event> T callEvent(@NonNull T event) {
    return CloudNetDriver.instance().eventManager().callEvent(event);
  }

  protected Object[] moduleArguments(ModuleConfiguration configuration) {
    return new String[]{configuration.group(),
      configuration.name(),
      configuration.version()};
  }
}
