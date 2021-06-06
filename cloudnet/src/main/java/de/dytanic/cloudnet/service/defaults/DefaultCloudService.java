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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.conf.ConfigurationOptionSSL;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientDriverAPI;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.handler.CloudServiceHandler;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public abstract class DefaultCloudService extends DefaultEmptyCloudService {

  protected static final char TEMP_NAME_SPLITTER = '_';
  protected static final long SERVICE_ERROR_RESTART_DELAY = 30;
  private static final Lock START_SEQUENCE_LOCK = new ReentrantLock();

  protected final Lock lifeCycleLock = new ReentrantLock();
  private final Path directory;
  protected boolean firstStartupOnStaticService = false;
  private boolean initialized;
  private boolean shutdownState;

  public DefaultCloudService(@NotNull String runtime, @NotNull ICloudServiceManager cloudServiceManager,
    @NotNull ServiceConfiguration serviceConfiguration, @NotNull CloudServiceHandler handler) {
    super(runtime, cloudServiceManager, serviceConfiguration, handler);
    this.directory = serviceConfiguration.isStaticService()
      ? cloudServiceManager.getPersistentServicesDirectoryPath().resolve(this.getServiceId().getName())
      : cloudServiceManager.getTempDirectoryPath()
        .resolve(this.getServiceId().getName() + TEMP_NAME_SPLITTER + this.getServiceId().getUniqueId());

    if (this.serviceConfiguration.isStaticService()) {
      this.firstStartupOnStaticService = Files.notExists(this.directory);
    }

    FileUtils.createDirectoryReported(this.directory);
  }

  @NotNull
  @Override
  public File getDirectory() {
    return this.directory.toFile();
  }

  @Override
  public @NotNull Path getDirectoryPath() {
    return this.directory;
  }

  @Override
  @ApiStatus.Internal
  public void init() {
    Preconditions.checkArgument(!this.initialized, "Cannot initialize a service twice");
    this.initialized = true;
    this.serviceInfoSnapshot = this.lastServiceInfoSnapshot = this.createServiceInfoSnapshot(ServiceLifeCycle.DEFINED);
    this.initAndPrepareService();
  }

  protected void initAndPrepareService() {
    if (this.lifeCycle == ServiceLifeCycle.DEFINED || this.lifeCycle == ServiceLifeCycle.STOPPED) {
      if (!this.prePrepare()) {
        return;
      }

      Path wrapperPath = this.directory.resolve(".wrapper");
      FileUtils.createDirectoryReported(wrapperPath);

      if (CloudNet.getInstance().getConfig().getServerSslConfig().isEnabled()) {
        try {
          Path certificatePath = wrapperPath.resolve("certificate");
          ConfigurationOptionSSL ssl = CloudNet.getInstance().getConfig().getServerSslConfig();

          Path path;
          if (ssl.getCertificatePath() != null) {
            if (Files.exists(path = Paths.get(ssl.getCertificatePath()))) {
              FileUtils.copy(path, certificatePath);
            }
          }

          if (ssl.getPrivateKeyPath() != null) {
            if (Files.exists(path = Paths.get(ssl.getPrivateKeyPath()))) {
              FileUtils.copy(path, certificatePath);
            }
          }

          if (ssl.getTrustCertificatePath() != null) {
            if (Files.exists(path = Paths.get(ssl.getTrustCertificatePath()))) {
              FileUtils.copy(path, certificatePath);
            }
          }
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }

      this.postPrepare();
    }
  }

  protected boolean checkEnoughResources() {
    if (this.cloudServiceManager.getCurrentUsedHeapMemory() + this.getConfiguredMaxHeapMemory() >= CloudNet
      .getInstance().getConfig().getMaxMemory()) {
      if (CloudNet.getInstance().getConfig().isRunBlockedServiceStartTryLaterAutomatic()) {
        CloudNet.getInstance().runTask(() -> {
          try {
            this.start();
          } catch (Exception exception) {
            exception.printStackTrace();
          }
        });
      } else {
        System.out.println(LanguageManager.getMessage("cloud-service-manager-max-memory-error"));
      }

      return false;
    }

    if (CPUUsageResolver.getSystemCPUUsage() >= CloudNet.getInstance().getConfig().getMaxCPUUsageToStartServices()) {
      if (CloudNet.getInstance().getConfig().isRunBlockedServiceStartTryLaterAutomatic()) {
        CloudNet.getInstance().runTask(() -> {
          try {
            this.start();
          } catch (Exception exception) {
            exception.printStackTrace();
          }
        });
      } else {
        System.out.println(LanguageManager.getMessage("cloud-service-manager-cpu-usage-to-high-error"));
      }

      return false;
    }

    return true;
  }

  protected ServiceInfoSnapshot createServiceInfoSnapshot(ServiceLifeCycle lifeCycle) {
    JsonDocument properties = this.serviceConfiguration.getProperties();
    if (lifeCycle != ServiceLifeCycle.STOPPED && this.serviceInfoSnapshot != null) {
      properties = this.serviceInfoSnapshot.getProperties();
    }
    return new ServiceInfoSnapshot(
      this.serviceInfoSnapshot == null ? System.currentTimeMillis() : this.serviceInfoSnapshot.getCreationTime(),
      new HostAndPort(CloudNet.getInstance().getConfig().getHostAddress(), this.serviceConfiguration.getPort()),
      new HostAndPort(CloudNet.getInstance().getConfig().getConnectHostAddress(), this.serviceConfiguration.getPort()),
      this.serviceInfoSnapshot == null ? -1 : this.serviceInfoSnapshot.getConnectedTime(),
      lifeCycle,
      this.serviceInfoSnapshot != null && this.isAlive() ? this.serviceInfoSnapshot.getProcessSnapshot()
        : ProcessSnapshot.empty(),
      properties,
      this.serviceConfiguration
    );
  }

  @Override
  public ITask<ServiceInfoSnapshot> forceUpdateServiceInfoSnapshotAsync() {
    if (this.getNetworkChannel() == null) {
      return CompletedTask.create(null);
    }

    return this.getNetworkChannel()
      .sendQueryAsync(new PacketClientDriverAPI(DriverAPIRequestType.FORCE_UPDATE_SERVICE))
      .map(packet -> packet.getBuffer().readObject(ServiceInfoSnapshot.class))
      .onComplete(serviceInfoSnapshot -> {
        if (serviceInfoSnapshot != null) {
          this.updateServiceInfoSnapshot(serviceInfoSnapshot);
        }
      });
  }

  @Override
  public void updateServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    this.setServiceInfoSnapshot(serviceInfoSnapshot);
    this.getCloudServiceManager()
      .handleServiceUpdate(PacketClientServerServiceInfoPublisher.PublisherType.UPDATE, serviceInfoSnapshot);

    CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot,
      PacketClientServerServiceInfoPublisher.PublisherType.UPDATE));
  }

  @Override
  public int stop() {
    return this.shutdown(false);
  }

  @Override
  public int kill() {
    return this.shutdown(true);
  }

  @Override
  public void start() throws Exception {
    if (!CloudNet.getInstance().getConfig().isParallelServiceStartSequence()) {
      try {
        START_SEQUENCE_LOCK.lock();
        this.startNow();
      } finally {
        START_SEQUENCE_LOCK.unlock();
      }
    } else {
      this.startNow();
    }
  }

  private int shutdown(boolean force) {
    if (this.shutdownState) {
      return -1;
    }
    this.shutdownState = true;
    int exitValue = this.shutdownNow(force);
    this.shutdownState = false;
    return exitValue;
  }

  protected abstract int shutdownNow(boolean force);

  protected abstract void startProcess() throws Exception;

  protected abstract void writeConfiguration();

  protected void startNow() throws Exception {
    try {
      this.lifeCycleLock.lock();
      this.invokeStart();
    } finally {
      this.lifeCycleLock.unlock();
    }
  }

  protected void invokeStart() throws Exception {
    if (this.lifeCycle == ServiceLifeCycle.PREPARED || this.lifeCycle == ServiceLifeCycle.STOPPED) {
      if (!this.prePrepareStart()) {
        return;
      }

      this.prepareStart();
      this.postPrepareStart();

      this.preStart();
      this.startProcess();
      this.postStart();
    }
  }

  protected void prepareStart() {
    this.includeInclusions();
    this.includeTemplates();

    this.serviceInfoSnapshot = this.createServiceInfoSnapshot(ServiceLifeCycle.PREPARED);
    this.getCloudServiceManager()
      .handleServiceUpdate(PacketClientServerServiceInfoPublisher.PublisherType.STARTED, this.serviceInfoSnapshot);

    this.writeConfiguration();
  }

  protected boolean prePrepare() {
    return super.handler.handlePrePrepare(this);
  }

  protected void postPrepare() {
    this.lifeCycle = ServiceLifeCycle.PREPARED;
    this.serviceInfoSnapshot.setLifeCycle(ServiceLifeCycle.PREPARED);

    super.handler.handlePostPrepare(this);
  }

  protected void preStart() {
    super.handler.handlePreStart(this);
  }

  protected void postStart() {
    this.lifeCycle = ServiceLifeCycle.RUNNING;
    this.serviceInfoSnapshot.setLifeCycle(ServiceLifeCycle.RUNNING);

    CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(this.serviceInfoSnapshot,
      PacketClientServerServiceInfoPublisher.PublisherType.STARTED));

    super.handler.handlePostStart(this);
  }

  protected boolean prePrepareStart() {
    if (!this.checkEnoughResources()) {
      return false;
    }

    return super.handler.handlePrePrepareStart(this);
  }

  protected void postPrepareStart() {
    super.handler.handlePostPrepareStart(this);
  }

  protected boolean preStop() {
    return super.handler.handlePreStop(this);
  }

  protected void postStop(int exitValue) {
    this.lifeCycle = ServiceLifeCycle.STOPPED;

    if (this.getServiceConfiguration().getDeletedFilesAfterStop() != null) {
      for (String path : this.getServiceConfiguration().getDeletedFilesAfterStop()) {
        Path file = this.directory.resolve(path);
        if (Files.exists(file)) {
          FileUtils.delete(file);
        }
      }
    }

    this.serviceInfoSnapshot = this.createServiceInfoSnapshot(ServiceLifeCycle.STOPPED);

    CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(this.serviceInfoSnapshot,
      PacketClientServerServiceInfoPublisher.PublisherType.STOPPED));

    super.handler.handlePostStop(this, exitValue);
  }

  protected void deleteFiles(boolean sendUpdate) {
    if (!this.preDelete()) {
      return;
    }

    this.deployResources();

    if (!this.getServiceConfiguration().isStaticService()) {
      FileUtils.delete(this.directory);
    }

    this.postDelete(sendUpdate);
  }

  protected boolean preDelete() {
    return super.handler.handlePreDelete(this);
  }

  protected void postDelete(boolean sendUpdate) {
    this.getCloudServiceManager().getCloudServices().remove(this.getServiceId().getUniqueId());
    this.getCloudServiceManager().handleServiceUpdate(PacketClientServerServiceInfoPublisher.PublisherType.UNREGISTER,
      this.getServiceInfoSnapshot());

    this.lifeCycle = ServiceLifeCycle.DELETED;
    this.serviceInfoSnapshot.setLifeCycle(ServiceLifeCycle.DELETED);

    if (sendUpdate) {
      CloudNet.getInstance().publishNetworkClusterNodeInfoSnapshotUpdate();
      CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(this.getServiceInfoSnapshot(),
        PacketClientServerServiceInfoPublisher.PublisherType.UNREGISTER));
    }

    super.handler.handlePostDelete(this);
  }
}
