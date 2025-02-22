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

package eu.cloudnetservice.driver.impl.module;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.module.ModulePostInstallDependencyEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePostLoadEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePostReloadEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePostStartEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePostStopEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePostUnloadEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePreInstallDependencyEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePreLoadEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePreReloadEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePreStartEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePreStopEvent;
import eu.cloudnetservice.driver.event.events.module.ModulePreUnloadEvent;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.module.ModuleConfiguration;
import eu.cloudnetservice.driver.module.ModuleDependency;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleProviderHandler;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the default implementation of the module provider handler.
 *
 * @see ModuleProviderHandler
 * @since 4.0
 */
@Singleton
public class DefaultModuleProviderHandler implements ModuleProviderHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModuleProviderHandler.class);

  protected final EventManager eventManager;
  protected final ModuleProvider moduleProvider;
  protected final ServiceRegistry serviceRegistry;

  @Inject
  protected DefaultModuleProviderHandler(
    @NonNull EventManager eventManager,
    @NonNull ModuleProvider moduleProvider,
    @NonNull ServiceRegistry serviceRegistry
  ) {
    this.eventManager = eventManager;
    this.moduleProvider = moduleProvider;
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handlePreModuleLoad(@NonNull ModuleWrapper moduleWrapper) {
    return !this.callEvent(new ModulePreLoadEvent(this.moduleProvider, moduleWrapper)).cancelled();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handlePostModuleLoad(@NonNull ModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostLoadEvent(this.moduleProvider, moduleWrapper));
    LOGGER.info(I18n.i18n().translate("cloudnet-post-load-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handlePreModuleStart(@NonNull ModuleWrapper moduleWrapper) {
    var cancelled = this.callEvent(new ModulePreStartEvent(this.moduleProvider, moduleWrapper)).cancelled();
    if (!cancelled) {
      this.eventManager.registerListener(moduleWrapper.module());
    }
    return !cancelled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handlePostModuleStart(@NonNull ModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostStartEvent(this.moduleProvider, moduleWrapper));
    LOGGER.info(I18n.i18n().translate("cloudnet-post-start-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handlePreModuleReload(@NonNull ModuleWrapper moduleWrapper) {
    return !this.callEvent(new ModulePreReloadEvent(this.moduleProvider, moduleWrapper)).cancelled();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handlePostModuleReload(@NonNull ModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostReloadEvent(this.moduleProvider, moduleWrapper));
    LOGGER.info(
      I18n.i18n().translate("cloudnet-post-reload-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handlePreModuleStop(@NonNull ModuleWrapper moduleWrapper) {
    return !this.callEvent(new ModulePreStopEvent(this.moduleProvider, moduleWrapper)).cancelled();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handlePostModuleStop(@NonNull ModuleWrapper moduleWrapper) {
    this.serviceRegistry.unregisterAll(moduleWrapper.classLoader());
    this.eventManager.unregisterListeners(moduleWrapper.classLoader());

    this.callEvent(new ModulePostStopEvent(this.moduleProvider, moduleWrapper));
    LOGGER.info(I18n.i18n().translate("cloudnet-post-stop-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handlePreModuleUnload(@NonNull ModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePreUnloadEvent(this.moduleProvider, moduleWrapper));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handlePostModuleUnload(@NonNull ModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostUnloadEvent(this.moduleProvider, moduleWrapper));
    LOGGER.info(
      I18n.i18n().translate("cloudnet-post-unload-module", this.moduleArguments(moduleWrapper.moduleConfiguration())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handlePreInstallDependency(
    @NonNull ModuleConfiguration configuration,
    @NonNull ModuleDependency dependency
  ) {
    this.callEvent(new ModulePreInstallDependencyEvent(this.moduleProvider, configuration, dependency));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handlePostInstallDependency(
    @NonNull ModuleConfiguration configuration,
    @NonNull ModuleDependency dependency
  ) {
    this.callEvent(new ModulePostInstallDependencyEvent(this.moduleProvider, configuration, dependency));
    LOGGER.debug("Dependency {}:{}:{} for module {}:{}:{} was successfully installed",
      dependency.group(),
      dependency.name(),
      dependency.version(),
      configuration.group(),
      configuration.name(),
      configuration.version());
  }

  /**
   * Shortcut for calling an event using {@link EventManager#callEvent(Event)}
   *
   * @param event the event to call.
   * @param <T>   the method parameter of the event
   * @return the same given event instance.
   * @throws NullPointerException if event is null.
   */
  protected @NonNull <T extends Event> T callEvent(@NonNull T event) {
    return this.eventManager.callEvent(event);
  }

  /**
   * Creates a new object array with these properties of the module:
   * <ol>
   *   <li>group</li>
   *   <li>name</li>
   *   <li>version</li>
   * </ol>
   *
   * @param configuration the module configuration to obtain the information from.
   * @return an object array containing the group, name and version of the module.
   * @throws NullPointerException if configuration is null.
   */
  protected Object[] moduleArguments(@NonNull ModuleConfiguration configuration) {
    return new String[]{configuration.group(),
      configuration.name(),
      configuration.version()};
  }
}
