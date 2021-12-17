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

package de.dytanic.cloudnet.driver.module;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostInstallDependencyEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostLoadEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostReloadEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostStartEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostStopEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostUnloadEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreInstallDependencyEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreLoadEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreReloadEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreStartEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreStopEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreUnloadEvent;
import org.jetbrains.annotations.NotNull;

public class DefaultModuleProviderHandler implements IModuleProviderHandler {

  private static final Logger LOGGER = LogManager.logger(DefaultModuleProviderHandler.class);

  @Override
  public boolean handlePreModuleLoad(@NotNull IModuleWrapper moduleWrapper) {
    var cancelled = this.callEvent(new ModulePreLoadEvent(this.moduleProvider(), moduleWrapper)).cancelled();
    if (!cancelled) {
      LOGGER.info(this.replaceAll(
        I18n.trans("cloudnet-pre-load-module"),
        this.moduleProvider(),
        moduleWrapper.moduleConfiguration()));
    }

    return !cancelled;
  }

  @Override
  public void handlePostModuleLoad(@NotNull IModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostLoadEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.fine(this.replaceAll(
      I18n.trans("cloudnet-post-load-module"),
      this.moduleProvider(),
      moduleWrapper.moduleConfiguration()));
  }

  @Override
  public boolean handlePreModuleStart(@NotNull IModuleWrapper moduleWrapper) {
    var cancelled = this.callEvent(new ModulePreStartEvent(this.moduleProvider(), moduleWrapper)).cancelled();
    if (!cancelled) {
      LOGGER.info(this.replaceAll(
        I18n.trans("cloudnet-pre-start-module"),
        this.moduleProvider(),
        moduleWrapper.moduleConfiguration()));
      CloudNetDriver.instance().eventManager().registerListener(moduleWrapper.module());
    }

    return !cancelled;
  }

  @Override
  public void handlePostModuleStart(@NotNull IModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostStartEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.fine(this.replaceAll(
      I18n.trans("cloudnet-post-start-module"),
      this.moduleProvider(),
      moduleWrapper.moduleConfiguration()));
  }

  @Override
  public boolean handlePreModuleReload(@NotNull IModuleWrapper moduleWrapper) {
    var cancelled = this.callEvent(new ModulePreReloadEvent(this.moduleProvider(), moduleWrapper)).cancelled();
    if (!cancelled) {
      LOGGER.info(this.replaceAll(
        I18n.trans("cloudnet-pre-reload-module"),
        this.moduleProvider(),
        moduleWrapper.moduleConfiguration()));
    }

    return !cancelled;
  }

  @Override
  public void handlePostModuleReload(@NotNull IModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostReloadEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.fine(this.replaceAll(
      I18n.trans("cloudnet-post-reload-module"),
      this.moduleProvider(),
      moduleWrapper.moduleConfiguration()));
  }


  @Override
  public boolean handlePreModuleStop(@NotNull IModuleWrapper moduleWrapper) {
    var cancelled = this.callEvent(new ModulePreStopEvent(this.moduleProvider(), moduleWrapper)).cancelled();
    if (!cancelled) {
      LOGGER.info(this.replaceAll(
        I18n.trans("cloudnet-pre-stop-module"),
        this.moduleProvider(),
        moduleWrapper.moduleConfiguration()));
    }

    return !cancelled;
  }

  @Override
  public void handlePostModuleStop(@NotNull IModuleWrapper moduleWrapper) {
    CloudNetDriver.instance().servicesRegistry().unregisterAll(moduleWrapper.classLoader());
    CloudNetDriver.instance().eventManager().unregisterListeners(moduleWrapper.classLoader());

    this.callEvent(new ModulePostStopEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.fine(this.replaceAll(
      I18n.trans("cloudnet-post-stop-module"),
      this.moduleProvider(),
      moduleWrapper.moduleConfiguration()));
  }

  @Override
  public void handlePreModuleUnload(@NotNull IModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePreUnloadEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.info(this.replaceAll(
      I18n.trans("cloudnet-pre-unload-module"),
      this.moduleProvider(),
      moduleWrapper.moduleConfiguration()));
  }

  @Override
  public void handlePostModuleUnload(@NotNull IModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostUnloadEvent(this.moduleProvider(), moduleWrapper));
    LOGGER.fine(this.replaceAll(
      I18n.trans("cloudnet-post-unload-module"),
      this.moduleProvider(),
      moduleWrapper.moduleConfiguration()));
  }

  @Override
  public void handlePreInstallDependency(
    @NotNull ModuleConfiguration configuration,
    @NotNull ModuleDependency dependency
  ) {
    this.callEvent(new ModulePreInstallDependencyEvent(this.moduleProvider(), configuration, dependency));
    LOGGER.fine(this.replaceAll(I18n.trans("cloudnet-pre-install-dependency-module")
        .replace("%group%", dependency.group())
        .replace("%name%", dependency.name())
        .replace("%version%", dependency.version()),
      this.moduleProvider(), configuration));
  }

  @Override
  public void handlePostInstallDependency(
    @NotNull ModuleConfiguration configuration,
    @NotNull ModuleDependency dependency
  ) {
    this.callEvent(new ModulePostInstallDependencyEvent(this.moduleProvider(), configuration, dependency));
    LOGGER.fine(this.replaceAll(I18n.trans("cloudnet-post-install-dependency-module")
        .replace("%group%", dependency.group())
        .replace("%name%", dependency.name())
        .replace("%version%", dependency.version()),
      this.moduleProvider(), configuration));
  }

  protected IModuleProvider moduleProvider() {
    return CloudNetDriver.instance().moduleProvider();
  }

  protected @NotNull <T extends Event> T callEvent(@NotNull T event) {
    return CloudNetDriver.instance().eventManager().callEvent(event);
  }

  protected String replaceAll(String text, IModuleProvider moduleProvider, ModuleConfiguration configuration) {
    Preconditions.checkNotNull(text);
    Preconditions.checkNotNull(moduleProvider);
    Preconditions.checkNotNull(configuration);

    return text.replace("%module_group%", configuration.group())
      .replace("%module_name%", configuration.name())
      .replace("%module_version%", configuration.version())
      .replace("%module_author%", configuration.author() == null ? "" : configuration.author());
  }
}
