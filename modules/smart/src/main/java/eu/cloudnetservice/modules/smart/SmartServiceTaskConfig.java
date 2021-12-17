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

package eu.cloudnetservice.modules.smart;

import lombok.NonNull;

public record SmartServiceTaskConfig(
  boolean enabled,
  int priority,
  int maxServices,
  int preparedServices,
  int smartMinServiceCount,
  boolean splitLogicallyOverNodes,
  boolean directTemplatesAndInclusionsSetup,
  @NonNull TemplateInstaller templateInstaller,
  int autoStopTimeByUnusedServiceInSeconds,
  int percentOfPlayersToCheckShouldStopTheService,
  int forAnewInstanceDelayTimeInSeconds,
  int percentOfPlayersForANewServiceByInstance
) implements Comparable<SmartServiceTaskConfig> {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull SmartServiceTaskConfig config) {
    return builder()
      .enabled(config.enabled())
      .priority(config.priority())

      .maxServices(config.maxServices())
      .preparedServices(config.preparedServices())
      .smartMinServiceCount(config.smartMinServiceCount())
      .splitLogicallyOverNodes(config.splitLogicallyOverNodes())

      .directTemplatesAndInclusionsSetup(config.directTemplatesAndInclusionsSetup())
      .templateInstaller(config.templateInstaller())

      .autoStopTimeByUnusedServiceInSeconds(config.autoStopTimeByUnusedServiceInSeconds())
      .percentOfPlayersToCheckShouldStop(config.percentOfPlayersToCheckShouldStopTheService())

      .forAnewInstanceDelayTimeInSeconds(config.forAnewInstanceDelayTimeInSeconds())
      .percentOfPlayersForANewServiceByInstance(config.percentOfPlayersForANewServiceByInstance());
  }

  @Override
  public int compareTo(@NonNull SmartServiceTaskConfig other) {
    return Integer.compare(this.priority, other.priority);
  }

  public enum TemplateInstaller {

    INSTALL_ALL,
    INSTALL_RANDOM,
    INSTALL_RANDOM_ONCE,
    INSTALL_BALANCED
  }

  public static class Builder {

    private boolean enabled = false;
    private int priority = 10;

    private int maxServices = -1;
    private int preparedServices = 0;
    private int smartMinServiceCount = 0;
    private boolean splitLogicallyOverNodes = true;

    private boolean directTemplatesAndInclusionsSetup = true;
    private TemplateInstaller templateInstaller = TemplateInstaller.INSTALL_ALL;

    private int autoStopTimeByUnusedServiceInSeconds = 180;
    private int percentOfPlayersToCheckShouldStopTheService = 0;

    private int forAnewInstanceDelayTimeInSeconds = 300;
    private int percentOfPlayersForANewServiceByInstance = 100;

    public @NonNull Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public @NonNull Builder priority(int priority) {
      this.priority = priority;
      return this;
    }

    public @NonNull Builder maxServices(int maxServices) {
      this.maxServices = maxServices;
      return this;
    }

    public @NonNull Builder preparedServices(int preparedServices) {
      this.preparedServices = preparedServices;
      return this;
    }

    public @NonNull Builder smartMinServiceCount(int smartMinServiceCount) {
      this.smartMinServiceCount = smartMinServiceCount;
      return this;
    }

    public @NonNull Builder splitLogicallyOverNodes(boolean splitLogicallyOverNodes) {
      this.splitLogicallyOverNodes = splitLogicallyOverNodes;
      return this;
    }

    public @NonNull Builder directTemplatesAndInclusionsSetup(boolean directTemplatesAndInclusionsSetup) {
      this.directTemplatesAndInclusionsSetup = directTemplatesAndInclusionsSetup;
      return this;
    }

    public @NonNull Builder templateInstaller(@NonNull TemplateInstaller templateInstaller) {
      this.templateInstaller = templateInstaller;
      return this;
    }

    public @NonNull Builder autoStopTimeByUnusedServiceInSeconds(int autoStopTimeByUnusedServiceInSeconds) {
      this.autoStopTimeByUnusedServiceInSeconds = autoStopTimeByUnusedServiceInSeconds;
      return this;
    }

    public @NonNull Builder percentOfPlayersToCheckShouldStop(int percentOfPlayersToCheckShouldStopTheService) {
      this.percentOfPlayersToCheckShouldStopTheService = percentOfPlayersToCheckShouldStopTheService;
      return this;
    }

    public @NonNull Builder forAnewInstanceDelayTimeInSeconds(int forAnewInstanceDelayTimeInSeconds) {
      this.forAnewInstanceDelayTimeInSeconds = forAnewInstanceDelayTimeInSeconds;
      return this;
    }

    public @NonNull Builder percentOfPlayersForANewServiceByInstance(int percentOfPlayersForANewServiceByInstance) {
      this.percentOfPlayersForANewServiceByInstance = percentOfPlayersForANewServiceByInstance;
      return this;
    }

    public @NonNull SmartServiceTaskConfig build() {
      return new SmartServiceTaskConfig(
        this.enabled,
        this.priority,
        this.maxServices,
        this.preparedServices,
        this.smartMinServiceCount,
        this.splitLogicallyOverNodes,
        this.directTemplatesAndInclusionsSetup,
        this.templateInstaller,
        this.autoStopTimeByUnusedServiceInSeconds,
        this.percentOfPlayersToCheckShouldStopTheService,
        this.forAnewInstanceDelayTimeInSeconds,
        this.percentOfPlayersForANewServiceByInstance);
    }
  }
}
