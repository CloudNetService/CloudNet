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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class SmartServiceTaskConfig implements Comparable<SmartServiceTaskConfig> {

  protected final boolean enabled;
  protected final int priority;

  protected final int maxServices;
  protected final int preparedServices;
  protected final int smartMinServiceCount;
  protected final boolean splitLogicallyOverNodes;

  protected final boolean directTemplatesAndInclusionsSetup;
  protected final TemplateInstaller templateInstaller;

  protected final int autoStopTimeByUnusedServiceInSeconds;
  protected final int percentOfPlayersToCheckShouldStopTheService;

  protected final int forAnewInstanceDelayTimeInSeconds;
  protected final int percentOfPlayersForANewServiceByInstance;

  protected SmartServiceTaskConfig(
    boolean enabled,
    int priority,
    int maxServices,
    int preparedServices,
    int smartMinServiceCount,
    boolean splitLogicallyOverNodes,
    boolean directTemplatesAndInclusionsSetup,
    @NotNull TemplateInstaller templateInstaller,
    int autoStopTimeByUnusedServiceInSeconds,
    int percentOfPlayersToCheckShouldStopTheService,
    int forAnewInstanceDelayTimeInSeconds,
    int percentOfPlayersForANewServiceByInstance
  ) {
    this.enabled = enabled;
    this.priority = priority;
    this.maxServices = maxServices;
    this.preparedServices = preparedServices;
    this.smartMinServiceCount = smartMinServiceCount;
    this.splitLogicallyOverNodes = splitLogicallyOverNodes;
    this.directTemplatesAndInclusionsSetup = directTemplatesAndInclusionsSetup;
    this.templateInstaller = templateInstaller;
    this.autoStopTimeByUnusedServiceInSeconds = autoStopTimeByUnusedServiceInSeconds;
    this.percentOfPlayersToCheckShouldStopTheService = percentOfPlayersToCheckShouldStopTheService;
    this.forAnewInstanceDelayTimeInSeconds = forAnewInstanceDelayTimeInSeconds;
    this.percentOfPlayersForANewServiceByInstance = percentOfPlayersForANewServiceByInstance;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull SmartServiceTaskConfig config) {
    return builder()
      .enabled(config.isEnabled())
      .priority(config.getPriority())

      .maxServices(config.getMaxServices())
      .preparedServices(config.getPreparedServices())
      .smartMinServiceCount(config.getSmartMinServiceCount())
      .splitLogicallyOverNodes(config.isSplitLogicallyOverNodes())

      .directTemplatesAndInclusionsSetup(config.isDirectTemplatesAndInclusionsSetup())
      .templateInstaller(config.getTemplateInstaller())

      .autoStopTimeByUnusedServiceInSeconds(config.getAutoStopTimeByUnusedServiceInSeconds())
      .percentOfPlayersToCheckShouldStop(config.getPercentOfPlayersToCheckShouldStopTheService())

      .forAnewInstanceDelayTimeInSeconds(config.getForAnewInstanceDelayTimeInSeconds())
      .percentOfPlayersForANewServiceByInstance(config.getPercentOfPlayersForANewServiceByInstance());
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public int getPriority() {
    return this.priority;
  }

  public int getMaxServices() {
    return this.maxServices;
  }

  public int getPreparedServices() {
    return this.preparedServices;
  }

  public int getSmartMinServiceCount() {
    return this.smartMinServiceCount;
  }

  public boolean isSplitLogicallyOverNodes() {
    return this.splitLogicallyOverNodes;
  }

  public boolean isDirectTemplatesAndInclusionsSetup() {
    return this.directTemplatesAndInclusionsSetup;
  }

  public @NotNull TemplateInstaller getTemplateInstaller() {
    return this.templateInstaller;
  }

  public int getAutoStopTimeByUnusedServiceInSeconds() {
    return this.autoStopTimeByUnusedServiceInSeconds;
  }

  public int getPercentOfPlayersToCheckShouldStopTheService() {
    return this.percentOfPlayersToCheckShouldStopTheService;
  }

  public int getForAnewInstanceDelayTimeInSeconds() {
    return this.forAnewInstanceDelayTimeInSeconds;
  }

  public int getPercentOfPlayersForANewServiceByInstance() {
    return this.percentOfPlayersForANewServiceByInstance;
  }

  @Override
  public int compareTo(@NotNull SmartServiceTaskConfig other) {
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

    public @NotNull Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public @NotNull Builder priority(int priority) {
      this.priority = priority;
      return this;
    }

    public @NotNull Builder maxServices(int maxServices) {
      this.maxServices = maxServices;
      return this;
    }

    public @NotNull Builder preparedServices(int preparedServices) {
      this.preparedServices = preparedServices;
      return this;
    }

    public @NotNull Builder smartMinServiceCount(int smartMinServiceCount) {
      this.smartMinServiceCount = smartMinServiceCount;
      return this;
    }

    public @NotNull Builder splitLogicallyOverNodes(boolean splitLogicallyOverNodes) {
      this.splitLogicallyOverNodes = splitLogicallyOverNodes;
      return this;
    }

    public @NotNull Builder directTemplatesAndInclusionsSetup(boolean directTemplatesAndInclusionsSetup) {
      this.directTemplatesAndInclusionsSetup = directTemplatesAndInclusionsSetup;
      return this;
    }

    public @NotNull Builder templateInstaller(@NotNull TemplateInstaller templateInstaller) {
      this.templateInstaller = templateInstaller;
      return this;
    }

    public @NotNull Builder autoStopTimeByUnusedServiceInSeconds(int autoStopTimeByUnusedServiceInSeconds) {
      this.autoStopTimeByUnusedServiceInSeconds = autoStopTimeByUnusedServiceInSeconds;
      return this;
    }

    public @NotNull Builder percentOfPlayersToCheckShouldStop(int percentOfPlayersToCheckShouldStopTheService) {
      this.percentOfPlayersToCheckShouldStopTheService = percentOfPlayersToCheckShouldStopTheService;
      return this;
    }

    public @NotNull Builder forAnewInstanceDelayTimeInSeconds(int forAnewInstanceDelayTimeInSeconds) {
      this.forAnewInstanceDelayTimeInSeconds = forAnewInstanceDelayTimeInSeconds;
      return this;
    }

    public @NotNull Builder percentOfPlayersForANewServiceByInstance(int percentOfPlayersForANewServiceByInstance) {
      this.percentOfPlayersForANewServiceByInstance = percentOfPlayersForANewServiceByInstance;
      return this;
    }

    public @NotNull SmartServiceTaskConfig build() {
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
