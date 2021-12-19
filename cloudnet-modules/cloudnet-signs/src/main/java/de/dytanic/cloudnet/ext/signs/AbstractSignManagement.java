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

package de.dytanic.cloudnet.ext.signs;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoStateWatcher;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationTaskEntry;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSignManagement extends ServiceInfoStateWatcher {

  private static final Comparator<Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState>> ENTRY_NAME_COMPARATOR = Comparator
    .comparing(entry -> entry.getFirst().getName());
  private static final Comparator<Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState>> ENTRY_STATE_COMPARATOR = Comparator
    .comparingInt(entry -> entry.getSecond().getPriority());

  protected final Set<Sign> signs;
  private final AtomicInteger[] indexes = new AtomicInteger[]{
    new AtomicInteger(-1), //starting
    new AtomicInteger(-1) //search
  };

  public AbstractSignManagement() {
    Collection<Sign> signsFromNode = this.getSignsFromNode();
    this.signs = signsFromNode == null ? new HashSet<>() : signsFromNode.stream()
      .filter(sign -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups())
        .contains(sign.getProvidedGroup()))
      .collect(Collectors.toSet());

    super.includeExistingServices();
  }

  /**
   * @deprecated SignManagement should be accessed via the {@link de.dytanic.cloudnet.common.registry.IServicesRegistry}
   */
  @Deprecated
  public static AbstractSignManagement getInstance() {
    return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(AbstractSignManagement.class);
  }

  protected abstract void updateSignNext(@NotNull Sign sign, @NotNull SignLayout signLayout,
    @Nullable ServiceInfoSnapshot serviceInfoSnapshot);

  /**
   * Removes all signs that don't exist anymore
   */
  public abstract void cleanup();

  /**
   * Runs a task on the main thread of the current application
   *
   * @param runnable the task
   * @param delay    the delay the task should have
   */
  protected abstract void runTaskLater(@NotNull Runnable runnable, long delay);


  @Override
  protected void handleUpdate() {
    this.updateSigns();
  }

  @Override
  protected boolean shouldWatchService(ServiceInfoSnapshot serviceInfoSnapshot) {
    if (serviceInfoSnapshot != null) {

      ServiceEnvironmentType currentEnvironment = Wrapper.getInstance().getServiceId().getEnvironment();
      ServiceEnvironmentType serviceEnvironment = serviceInfoSnapshot.getServiceId().getEnvironment();

      return (serviceEnvironment.isMinecraftJavaServer() && currentEnvironment.isMinecraftJavaServer())
        || (serviceEnvironment.isMinecraftBedrockServer() && currentEnvironment.isMinecraftBedrockServer());
    }

    return false;
  }

  @Override
  protected boolean shouldShowFullServices() {
    return !this.getOwnSignConfigurationEntry().isSwitchToSearchingWhenServiceIsFull();
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equals(SignConstants.SIGN_CHANNEL_NAME) || event.getMessage() == null) {
      return;
    }

    switch (event.getMessage().toLowerCase()) {
      case SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION: {
        SignConfiguration signConfiguration = event.getData().get("signConfiguration", SignConfiguration.TYPE);
        SignConfigurationProvider.setLocal(signConfiguration);
      }
      break;
      case SignConstants.SIGN_CHANNEL_ADD_SIGN_MESSAGE: {
        Sign sign = event.getData().get("sign", Sign.TYPE);

        if (sign != null) {
          this.addSign(sign);
        }
      }
      break;
      case SignConstants.SIGN_CHANNEL_REMOVE_SIGN_MESSAGE: {
        Sign sign = event.getData().get("sign", Sign.TYPE);

        if (sign != null) {
          this.removeSign(sign);
        }
      }
      break;
      default:
        break;
    }
  }

  /**
   * Adds a sign to this wrapper instance
   *
   * @param sign the sign to add
   * @return if the sign is allowed to exist on this wrapper instance
   */
  public boolean addSign(@NotNull Sign sign) {
    if (Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(sign.getProvidedGroup())) {
      this.signs.add(sign);
      CloudNetDriver.getInstance().getTaskExecutor().execute(this::updateSigns);
      return true;
    }
    return false;
  }

  /**
   * Removes a sign from this wrapper instance
   *
   * @param sign the sign to remove
   */
  public void removeSign(@NotNull Sign sign) {
    this.signs.stream()
      .filter(filterSign -> filterSign.getSignId() == sign.getSignId())
      .findFirst().ifPresent(this.signs::remove);

    CloudNetDriver.getInstance().getTaskExecutor().execute(this::updateSigns);
  }

  public void updateSigns() {
    SignConfigurationEntry signConfiguration = this.getOwnSignConfigurationEntry();
    if (signConfiguration == null) {
      return;
    }

    List<Sign> signs = new ArrayList<>(this.signs);
    Collections.sort(signs);

    List<Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState>> entries = super.services.values().stream()
      .filter(pair -> pair.getSecond() != ServiceInfoStateWatcher.ServiceInfoState.STOPPED)
      .sorted(ENTRY_NAME_COMPARATOR)
      .collect(Collectors.toList());

    for (Sign sign : signs) {
      this.updateSign(sign, signConfiguration, entries);
    }
  }

  private void updateSign(Sign sign,
    SignConfigurationEntry signConfiguration,
    List<Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState>> entries) {

    Optional<Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState>> optionalEntry = entries.stream()
      .filter(entry -> {
        boolean access = Arrays.asList(entry.getFirst().getConfiguration().getGroups())
          .contains(sign.getTargetGroup());

        if (sign.getTemplatePath() != null) {
          boolean condition = false;

          for (ServiceTemplate template : entry.getFirst().getConfiguration().getTemplates()) {
            if (sign.getTemplatePath().equals(template.getTemplatePath())) {
              condition = true;
              break;
            }
          }

          access = condition;
        }

        return access;
      })
      .max(ENTRY_STATE_COMPARATOR);

    if (optionalEntry.isPresent()) {
      Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState> entry = optionalEntry.get();

      sign.setServiceInfoSnapshot(entry.getFirst());
      this.applyState(sign, signConfiguration, entry.getFirst(), entry.getSecond());

      entries.remove(entry);
    } else {
      sign.setServiceInfoSnapshot(null);

      if (!signConfiguration.getSearchLayouts().getSignLayouts().isEmpty()) {
        this.updateSignNext(sign, signConfiguration.getSearchLayouts().getSignLayouts().get(this.indexes[1].get()),
          null);
      }
    }
  }

  private void applyState(Sign sign, SignConfigurationEntry signConfiguration, ServiceInfoSnapshot serviceInfoSnapshot,
    ServiceInfoStateWatcher.ServiceInfoState state) {
    switch (state) {
      case STOPPED: {
        sign.setServiceInfoSnapshot(null);

        int searchingIndex = this.indexes[1].get();
        if (searchingIndex != -1) {
          this.updateSignNext(sign, signConfiguration.getSearchLayouts().getSignLayouts().get(searchingIndex), null);
        }
      }
      break;
      case STARTING: {
        sign.setServiceInfoSnapshot(null);

        int startingIndex = this.indexes[0].get();
        if (startingIndex != -1) {
          this.updateSignNext(sign, signConfiguration.getStartingLayouts().getSignLayouts().get(startingIndex),
            serviceInfoSnapshot);
        }
      }
      break;
      case EMPTY_ONLINE: {
        SignLayout signLayout = null;

        SignConfigurationTaskEntry taskEntry = this
          .getValidSignConfigurationTaskEntryFromSignConfigurationEntry(signConfiguration, sign.getTargetGroup());

        if (taskEntry != null) {
          signLayout = taskEntry.getEmptyLayout();
        }

        if (signLayout == null) {
          signLayout = signConfiguration.getDefaultEmptyLayout();
        }

        this.updateSignNext(sign, signLayout, serviceInfoSnapshot);
      }
      break;
      case ONLINE: {
        SignLayout signLayout = null;

        SignConfigurationTaskEntry taskEntry = this
          .getValidSignConfigurationTaskEntryFromSignConfigurationEntry(signConfiguration, sign.getTargetGroup());

        if (taskEntry != null) {
          signLayout = taskEntry.getOnlineLayout();
        }

        if (signLayout == null) {
          signLayout = signConfiguration.getDefaultOnlineLayout();
        }

        this.updateSignNext(sign, signLayout, serviceInfoSnapshot);
      }
      break;
      case FULL_ONLINE: {
        SignLayout signLayout = null;

        SignConfigurationTaskEntry taskEntry = this
          .getValidSignConfigurationTaskEntryFromSignConfigurationEntry(signConfiguration, sign.getTargetGroup());

        if (taskEntry != null) {
          signLayout = taskEntry.getFullLayout();
        }

        if (signLayout == null) {
          signLayout = signConfiguration.getDefaultFullLayout();
        }

        this.updateSignNext(sign, signLayout, serviceInfoSnapshot);
      }
      break;
      default:
        break;
    }
  }

  private SignConfigurationTaskEntry getValidSignConfigurationTaskEntryFromSignConfigurationEntry(
    SignConfigurationEntry entry, String targetTask) {
    return entry.getTaskLayouts().stream()
      .filter(signConfigurationTaskEntry -> signConfigurationTaskEntry.getTask() != null &&
        signConfigurationTaskEntry.getEmptyLayout() != null &&
        signConfigurationTaskEntry.getFullLayout() != null &&
        signConfigurationTaskEntry.getOnlineLayout() != null &&
        signConfigurationTaskEntry.getTask().equalsIgnoreCase(targetTask))
      .findFirst()
      .orElse(null);
  }

  public SignConfigurationEntry getOwnSignConfigurationEntry() {
    return SignConfigurationProvider.load().getConfigurations().stream()
      .filter(signConfigurationEntry -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups())
        .contains(signConfigurationEntry.getTargetGroup()))
      .findFirst()
      .orElse(null);
  }


  /**
   * Adds a sign to the whole cluster and the database
   *
   * @param sign the sign to add
   */
  public void sendSignAddUpdate(@NotNull Sign sign) {
    ChannelMessage.builder()
      .channel(SignConstants.SIGN_CHANNEL_NAME)
      .message(SignConstants.SIGN_CHANNEL_ADD_SIGN_MESSAGE)
      .json(new JsonDocument("sign", sign))
      .build()
      .send();
  }

  /**
   * Removes a sign from the whole cluster and the database
   *
   * @param sign the sign to remove
   */
  public void sendSignRemoveUpdate(@NotNull Sign sign) {
    ChannelMessage.builder()
      .channel(SignConstants.SIGN_CHANNEL_NAME)
      .message(SignConstants.SIGN_CHANNEL_REMOVE_SIGN_MESSAGE)
      .json(new JsonDocument("sign", sign))
      .build()
      .send();
  }

  /**
   * Returns all signs contained in the CloudNet sign database
   *
   * @return all signs or null, if an error occurred
   */
  @Nullable
  public Collection<Sign> getSignsFromNode() {
    ChannelMessage response = ChannelMessage.builder()
      .channel(SignConstants.SIGN_CHANNEL_NAME)
      .message(SignConstants.SIGN_CHANNEL_GET_SIGNS)
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQuery();

    return response == null ? null : response.getJson().get("signs", SignConstants.COLLECTION_SIGNS);
  }

  /**
   * Updates the SignConfiguration in the whole cluster
   *
   * @param signConfiguration the new SignConfiguration
   */
  public void updateSignConfiguration(@NotNull SignConfiguration signConfiguration) {
    ChannelMessage.builder()
      .channel(SignConstants.SIGN_CHANNEL_NAME)
      .message(SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION)
      .json(new JsonDocument("signConfiguration", signConfiguration))
      .build()
      .send();
  }

  protected void executeStartingTask() {
    SignConfigurationEntry signConfigurationEntry = this.getOwnSignConfigurationEntry();
    AtomicInteger startingIndex = this.indexes[0];

    if (signConfigurationEntry != null && signConfigurationEntry.getStartingLayouts() != null &&
      signConfigurationEntry.getStartingLayouts().getSignLayouts().size() > 0) {
      if (startingIndex.get() == -1) {
        startingIndex.set(0);
      }

      if ((startingIndex.get() + 1) < signConfigurationEntry.getStartingLayouts().getSignLayouts().size()) {
        startingIndex.incrementAndGet();
      } else {
        startingIndex.set(0);
      }

      this.runTaskLater(
        this::executeStartingTask,
        20 / (Math.min(signConfigurationEntry.getStartingLayouts().getAnimationsPerSecond(), 20))
      );
    } else {
      startingIndex.set(-1);
      this.runTaskLater(this::executeStartingTask, 20);
    }

    // CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
    this.updateSigns();
  }

  protected void executeSearchingTask() {
    SignConfigurationEntry signConfigurationEntry = this.getOwnSignConfigurationEntry();
    AtomicInteger searchingIndex = this.indexes[1];

    if (signConfigurationEntry != null && signConfigurationEntry.getSearchLayouts() != null &&
      signConfigurationEntry.getSearchLayouts().getSignLayouts().size() > 0) {
      if (searchingIndex.get() == -1) {
        searchingIndex.set(0);
      }

      if ((searchingIndex.get() + 1) < signConfigurationEntry.getSearchLayouts().getSignLayouts().size()) {
        searchingIndex.incrementAndGet();
      } else {
        searchingIndex.set(0);
      }

      this.runTaskLater(
        this::executeSearchingTask,
        20 / (Math.min(signConfigurationEntry.getSearchLayouts().getAnimationsPerSecond(), 20))
      );
    } else {
      searchingIndex.set(-1);
      this.runTaskLater(this::executeSearchingTask, 20);
    }

    // CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
    this.updateSigns();
  }

  public AtomicInteger[] getIndexes() {
    return this.indexes;
  }

  /**
   * Returns a copy of the signs allowed to exist on this wrapper instance Use {@link
   * AbstractSignManagement#addSign(Sign)} and {@link AbstractSignManagement#removeSign(Sign)} for local modification
   *
   * @return a copy of the signs
   */
  public Set<Sign> getSigns() {
    return new HashSet<>(this.signs);
  }

}
