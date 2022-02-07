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

package eu.cloudnetservice.cloudnet.node.service.defaults.provider;

import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.cloudnet.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.service.ServiceDeployment;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import java.util.Queue;
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
  public Queue<String> cachedLogMessages() {
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
  public void includeWaitingServiceTemplates() {
  }

  @Override
  public void includeWaitingServiceInclusions() {
  }

  @Override
  public void deployResources(boolean removeDeployments) {
  }
}
