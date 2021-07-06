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

package de.dytanic.cloudnet.service.defaults;

import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.handler.CloudServiceHandler;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultEmptyCloudService implements ICloudService {

  protected final ICloudServiceManager cloudServiceManager;
  protected final ServiceConfiguration serviceConfiguration;
  protected final CloudServiceHandler handler;
  private final String runtime;
  private final String connectionKey;
  protected volatile ServiceLifeCycle lifeCycle;
  protected volatile ServiceInfoSnapshot serviceInfoSnapshot;
  protected volatile ServiceInfoSnapshot lastServiceInfoSnapshot;
  private volatile INetworkChannel networkChannel;

  public DefaultEmptyCloudService(@NotNull String runtime, @NotNull ICloudServiceManager cloudServiceManager,
    @NotNull ServiceConfiguration serviceConfiguration, @NotNull CloudServiceHandler handler) {
    this.runtime = runtime;
    this.cloudServiceManager = cloudServiceManager;
    this.serviceConfiguration = serviceConfiguration;
    this.handler = handler;

    this.lifeCycle = ServiceLifeCycle.DEFINED;
    this.connectionKey = StringUtil.generateRandomString(256);
  }

  @NotNull
  @Override
  public String getRuntime() {
    return this.runtime;
  }

  @Override
  public @Nullable String getJavaCommand() {
    return this.serviceConfiguration.getJavaCommand();
  }

  @Override
  public int getConfiguredMaxHeapMemory() {
    return this.serviceConfiguration.getProcessConfig().getMaxHeapMemorySize();
  }

  @Override
  public @NotNull ServiceId getServiceId() {
    return this.serviceConfiguration.getServiceId();
  }

  @NotNull
  public ServiceLifeCycle getLifeCycle() {
    return this.lifeCycle;
  }

  @NotNull
  @Override
  public ServiceConfiguration getServiceConfiguration() {
    return this.serviceConfiguration;
  }

  @NotNull
  @Override
  public ICloudServiceManager getCloudServiceManager() {
    return this.cloudServiceManager;
  }

  @Override
  public List<String> getGroups() {
    return Arrays.asList(this.serviceConfiguration.getGroups());
  }

  @Override
  public INetworkChannel getNetworkChannel() {
    return this.networkChannel;
  }

  @Override
  public void setNetworkChannel(@NotNull INetworkChannel networkChannel) {
    this.networkChannel = networkChannel;
  }

  @NotNull
  @Override
  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }

  protected void setServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    this.lastServiceInfoSnapshot = this.serviceInfoSnapshot;
    this.serviceInfoSnapshot = serviceInfoSnapshot;
  }

  @NotNull
  @Override
  public ServiceInfoSnapshot getLastServiceInfoSnapshot() {
    return this.lastServiceInfoSnapshot;
  }

  @Override
  public String getConnectionKey() {
    return this.connectionKey;
  }

}
