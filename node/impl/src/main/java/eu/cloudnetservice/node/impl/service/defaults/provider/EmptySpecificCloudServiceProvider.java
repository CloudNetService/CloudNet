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

package eu.cloudnetservice.node.impl.service.defaults.provider;

import eu.cloudnetservice.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class EmptySpecificCloudServiceProvider implements SpecificCloudServiceProvider {

  public static final EmptySpecificCloudServiceProvider INSTANCE = new EmptySpecificCloudServiceProvider();

  private EmptySpecificCloudServiceProvider() {
  }

  @Override
  public @Nullable ServiceInfoSnapshot serviceInfo() {
    return null;
  }

  @Override
  public boolean valid() {
    return false;
  }

  @Override
  public @Nullable ServiceInfoSnapshot forceUpdateServiceInfo() {
    return null;
  }

  @Override
  public void addServiceTemplate(@NonNull ServiceTemplate serviceTemplate) {
  }

  @Override
  public void addServiceRemoteInclusion(@NonNull ServiceRemoteInclusion serviceRemoteInclusion) {
  }

  @Override
  public void addServiceDeployment(@NonNull ServiceDeployment serviceDeployment) {
  }

  @Override
  public @NonNull Queue<String> cachedLogMessages() {
    return new LinkedBlockingDeque<>();
  }

  @Override
  public boolean toggleScreenEvents(@NonNull ChannelMessageSender channelMessageSender, @NonNull String channel) {
    return false;
  }

  @Override
  public void deleteFiles() {

  }

  @Override
  public void updateLifecycle(@NonNull ServiceLifeCycle lifeCycle) {
  }

  @Override
  public void restart() {
  }

  @Override
  public void runCommand(@NonNull String command) {
  }

  @Override
  public @NonNull Collection<ServiceTemplate> installedTemplates() {
    return List.of();
  }

  @Override
  public @NonNull Collection<ServiceRemoteInclusion> installedInclusions() {
    return List.of();
  }

  @Override
  public @NonNull Collection<ServiceDeployment> installedDeployments() {
    return List.of();
  }

  @Override
  public void includeWaitingServiceTemplates() {
  }

  @Override
  public void includeWaitingServiceTemplates(boolean force) {
  }

  @Override
  public void includeWaitingServiceInclusions() {
  }

  @Override
  public void deployResources(boolean removeDeployments) {
  }

  @Override
  public void updateProperties(@NonNull Document properties) {
  }

  @Override
  public @NonNull CompletableFuture<ServiceInfoSnapshot> serviceInfoAsync() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Boolean> validAsync() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<ServiceInfoSnapshot> forceUpdateServiceInfoAsync() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> addServiceTemplateAsync(@NonNull ServiceTemplate serviceTemplate) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> addServiceRemoteInclusionAsync(
    @NonNull ServiceRemoteInclusion serviceRemoteInclusion
  ) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> addServiceDeploymentAsync(@NonNull ServiceDeployment serviceDeployment) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Queue<String>> cachedLogMessagesAsync() {
    return CompletableFuture.completedFuture(new LinkedBlockingDeque<>());
  }

  @Override
  public @NonNull CompletableFuture<Boolean> toggleScreenEventsAsync(@NonNull ChannelMessageSender sender,
    @NonNull String channel
  ) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> restartAsync() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> updateLifecycleAsync(@NonNull ServiceLifeCycle lifeCycle) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> deleteFilesAsync() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> runCommandAsync(@NonNull String command) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> includeWaitingServiceTemplatesAsync() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> includeWaitingServiceTemplatesAsync(boolean force) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> includeWaitingServiceInclusionsAsync() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> deployResourcesAsync(boolean removeDeployments) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> removeAndExecuteDeploymentsAsync() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NonNull CompletableFuture<Void> updatePropertiesAsync(@NonNull Document properties) {
    return CompletableFuture.completedFuture(null);
  }
}
