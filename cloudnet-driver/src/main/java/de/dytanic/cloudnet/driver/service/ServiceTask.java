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

package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class ServiceTask extends ServiceConfigurationBase implements INameable, SerializableObject {

  private String name;
  private String runtime;
  private String javaCommand;

  private boolean disableIpRewrite;
  private boolean maintenance;
  private boolean autoDeleteOnStop;
  private boolean staticServices;

  private Collection<String> associatedNodes = new ArrayList<>();
  private Collection<String> groups = new ArrayList<>();
  private Collection<String> deletedFilesAfterStop = new ArrayList<>();

  private ProcessConfiguration processConfiguration;

  private int startPort;
  private int minServiceCount = 0;

  /**
   * Represents the time in millis where this task is able to start new services again
   */
  private transient long serviceStartAbilityTime = -1;

  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes,
    Collection<String> groups,
    ProcessConfiguration processConfiguration, int startPort, int minServiceCount) {
    this(includes, templates, deployments, name, runtime, false, autoDeleteOnStop,
      staticServices, associatedNodes, groups, processConfiguration, startPort, minServiceCount);
  }

  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes,
    Collection<String> groups,
    Collection<String> deletedFilesAfterStop, ProcessConfiguration processConfiguration, int startPort,
    int minServiceCount) {
    this(includes, templates, deployments, name, runtime, false, autoDeleteOnStop, staticServices, associatedNodes,
      groups, deletedFilesAfterStop, processConfiguration, startPort, minServiceCount);
  }

  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes,
    Collection<String> groups,
    Collection<String> deletedFilesAfterStop, ProcessConfiguration processConfiguration, int startPort,
    int minServiceCount, String javaCommand) {
    this(includes, templates, deployments, name, runtime, false, autoDeleteOnStop, staticServices, associatedNodes,
      groups, deletedFilesAfterStop, processConfiguration, startPort, minServiceCount, javaCommand);
  }

  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean maintenance, boolean autoDeleteOnStop, boolean staticServices,
    Collection<String> associatedNodes, Collection<String> groups,
    ProcessConfiguration processConfiguration, int startPort, int minServiceCount) {
    this(includes, templates, deployments, name, runtime, maintenance, autoDeleteOnStop, staticServices,
      associatedNodes, groups, new ArrayList<>(), processConfiguration, startPort, minServiceCount);
  }

  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean maintenance, boolean autoDeleteOnStop, boolean staticServices,
    Collection<String> associatedNodes, Collection<String> groups,
    Collection<String> deletedFilesAfterStop, ProcessConfiguration processConfiguration, int startPort,
    int minServiceCount) {
    this(includes, templates, deployments, name, runtime, maintenance, autoDeleteOnStop, staticServices,
      associatedNodes, groups, deletedFilesAfterStop, processConfiguration, startPort, minServiceCount, null);
  }

  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean maintenance, boolean autoDeleteOnStop, boolean staticServices,
    Collection<String> associatedNodes, Collection<String> groups,
    Collection<String> deletedFilesAfterStop, ProcessConfiguration processConfiguration, int startPort,
    int minServiceCount, String javaCommand) {
    super(includes, templates, deployments);
    this.name = name;
    this.runtime = runtime;
    this.maintenance = maintenance;
    this.autoDeleteOnStop = autoDeleteOnStop;
    this.associatedNodes = associatedNodes;
    this.groups = groups;
    this.deletedFilesAfterStop = deletedFilesAfterStop;
    this.processConfiguration = processConfiguration;
    this.startPort = startPort;
    this.minServiceCount = minServiceCount;
    this.staticServices = staticServices;
    this.javaCommand = javaCommand;
  }

  public ServiceTask() {
  }

  @Override
  public Collection<String> getJvmOptions() {
    return this.processConfiguration.getJvmOptions();
  }

  @Override
  public Collection<String> getProcessParameters() {
    return this.processConfiguration.getProcessParameters();
  }

  /**
   * Forbids this task to auto start new services for a specific time on the current node. This method has no effect
   * when executed on a wrapper instances.
   *
   * @param time the time in millis
   */
  public void forbidServiceStarting(long time) {
    this.serviceStartAbilityTime = System.currentTimeMillis() + time;
  }

  public boolean canStartServices() {
    return !this.maintenance && System.currentTimeMillis() > this.serviceStartAbilityTime;
  }

  public boolean isDisableIpRewrite() {
    return this.disableIpRewrite;
  }

  public void setDisableIpRewrite(boolean disableIpRewrite) {
    this.disableIpRewrite = disableIpRewrite;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Nullable
  public String getJavaCommand() {
    return this.javaCommand;
  }

  public void setJavaCommand(String javaCommand) {
    this.javaCommand = javaCommand;
  }

  public String getRuntime() {
    return this.runtime;
  }

  public void setRuntime(String runtime) {
    this.runtime = runtime;
  }

  public boolean isMaintenance() {
    return this.maintenance;
  }

  public void setMaintenance(boolean maintenance) {
    this.maintenance = maintenance;
  }

  public boolean isAutoDeleteOnStop() {
    return this.autoDeleteOnStop;
  }

  public void setAutoDeleteOnStop(boolean autoDeleteOnStop) {
    this.autoDeleteOnStop = autoDeleteOnStop;
  }

  public boolean isStaticServices() {
    return this.staticServices;
  }

  public void setStaticServices(boolean staticServices) {
    this.staticServices = staticServices;
  }

  public Collection<String> getAssociatedNodes() {
    return this.associatedNodes;
  }

  public void setAssociatedNodes(Collection<String> associatedNodes) {
    this.associatedNodes = associatedNodes;
  }

  public Collection<String> getGroups() {
    return this.groups;
  }

  public void setGroups(Collection<String> groups) {
    this.groups = groups;
  }

  public Collection<String> getDeletedFilesAfterStop() {
    return this.deletedFilesAfterStop;
  }

  public void setDeletedFilesAfterStop(Collection<String> deletedFilesAfterStop) {
    this.deletedFilesAfterStop = deletedFilesAfterStop;
  }

  public ProcessConfiguration getProcessConfiguration() {
    return this.processConfiguration;
  }

  public void setProcessConfiguration(ProcessConfiguration processConfiguration) {
    this.processConfiguration = processConfiguration;
  }

  public int getStartPort() {
    return this.startPort;
  }

  public void setStartPort(int startPort) {
    this.startPort = startPort;
  }

  public int getMinServiceCount() {
    return this.minServiceCount;
  }

  public void setMinServiceCount(int minServiceCount) {
    this.minServiceCount = minServiceCount;
  }

  public ServiceTask makeClone() {
    return new ServiceTask(
      new ArrayList<>(this.includes),
      new ArrayList<>(this.templates),
      new ArrayList<>(this.deployments),
      this.name,
      this.runtime,
      this.maintenance,
      this.autoDeleteOnStop,
      this.staticServices,
      new ArrayList<>(this.associatedNodes),
      new ArrayList<>(this.groups),
      new ArrayList<>(this.deletedFilesAfterStop),
      new ProcessConfiguration(
        this.processConfiguration.getEnvironment(),
        this.processConfiguration.getMaxHeapMemorySize(),
        new ArrayList<>(this.processConfiguration.getJvmOptions()),
        new ArrayList<>(this.processConfiguration.getProcessParameters())
      ),
      this.startPort,
      this.minServiceCount,
      this.javaCommand
    );
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    super.write(buffer);
    buffer.writeString(this.name);
    buffer.writeString(this.runtime);
    buffer.writeOptionalString(this.javaCommand);
    buffer.writeBoolean(this.disableIpRewrite);
    buffer.writeBoolean(this.maintenance);
    buffer.writeBoolean(this.autoDeleteOnStop);
    buffer.writeBoolean(this.staticServices);
    buffer.writeStringCollection(this.associatedNodes);
    buffer.writeStringCollection(this.groups);
    buffer.writeStringCollection(this.deletedFilesAfterStop);
    buffer.writeObject(this.processConfiguration);
    buffer.writeInt(this.startPort);
    buffer.writeInt(this.minServiceCount);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    super.read(buffer);
    this.name = buffer.readString();
    this.runtime = buffer.readString();
    this.javaCommand = buffer.readOptionalString();
    this.disableIpRewrite = buffer.readBoolean();
    this.maintenance = buffer.readBoolean();
    this.autoDeleteOnStop = buffer.readBoolean();
    this.staticServices = buffer.readBoolean();
    this.associatedNodes = buffer.readStringCollection();
    this.groups = buffer.readStringCollection();
    this.deletedFilesAfterStop = buffer.readStringCollection();
    this.processConfiguration = buffer.readObject(ProcessConfiguration.class);
    this.startPort = buffer.readInt();
    this.minServiceCount = buffer.readInt();
  }
}
