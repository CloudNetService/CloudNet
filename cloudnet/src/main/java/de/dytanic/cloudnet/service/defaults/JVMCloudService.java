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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.handler.CloudServiceHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class JVMCloudService extends DefaultMinecraftCloudService implements ICloudService {

  protected static final String RUNTIME = "jvm";

  private final DefaultServiceConsoleLogCache serviceConsoleLogCache = new DefaultServiceConsoleLogCache(this);

  private Process process;
  private volatile boolean restartState = false;

  JVMCloudService(ICloudServiceManager cloudServiceManager, ServiceConfiguration serviceConfiguration,
    @NotNull CloudServiceHandler handler) {
    super(RUNTIME, cloudServiceManager, serviceConfiguration, handler);
  }

  @Override
  public void runCommand(@NotNull String commandLine) {
    if (this.lifeCycle == ServiceLifeCycle.RUNNING && this.process != null) {
      try {
        OutputStream outputStream = this.process.getOutputStream();
        outputStream.write((commandLine + "\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    }
  }

  @Override
  public void restart() throws Exception {
    this.restartState = true;

    this.stop();
    this.start();

    this.restartState = false;
  }

  @Override
  protected int shutdownNow(boolean force) {
    Integer exitCode = 0;

    try {
      this.lifeCycleLock.lock();
      return (exitCode = this.stop0(force)) == null ? -1 : exitCode;
    } finally {
      this.lifeCycleLock.unlock();

      if (exitCode != null) { // user didn't cancel stop event, exitCode is present
        this.invokeAutoDeleteOnStopIfNotRestart();
      }
    }
  }

  private void invokeAutoDeleteOnStopIfNotRestart() {
    if (this.getServiceConfiguration().isAutoDeleteOnStop() && !this.restartState) {
      this.delete();
    } else {
      this.initAndPrepareService();
    }
  }

  @Override
  public void delete(boolean sendUpdate) {
    try {
      this.lifeCycleLock.lock();
      this.delete0(sendUpdate);
    } finally {
      this.lifeCycleLock.unlock();
    }
  }

  @Override
  public boolean isAlive() {
    return this.lifeCycle == ServiceLifeCycle.DEFINED || this.lifeCycle == ServiceLifeCycle.PREPARED ||
      (this.lifeCycle == ServiceLifeCycle.RUNNING && this.process != null && this.process.isAlive());
  }

  @Override
  protected void writeConfiguration() {
    HostAndPort[] listeners = CloudNet.getInstance().getConfig().getIdentity().getListeners();
    JsonDocument.newDocument()
      .append("connectionKey", this.getConnectionKey())
      .append("listener", listeners[ThreadLocalRandom.current().nextInt(listeners.length)])
      .append("serviceConfiguration", this.getServiceConfiguration())
      .append("serviceInfoSnapshot", this.serviceInfoSnapshot)
      .append("sslConfig", CloudNet.getInstance().getConfig().getServerSslConfig())
      .write(this.getDirectoryPath().resolve(".wrapper/wrapper.json"));
  }

  @Override
  protected void startProcess() throws Exception {
    List<String> commandArguments = new ArrayList<>();

    String javaCommand = this.getJavaCommand();
    commandArguments.add(javaCommand == null ? CloudNet.getInstance().getConfig().getJVMCommand() : javaCommand);

    commandArguments.addAll(CloudNet.getInstance().getConfig().getDefaultJVMFlags().getJvmFlags());

    commandArguments.addAll(Arrays.asList(
      // sys properties
      "-Djline.terminal=jline.UnsupportedTerminal",
      "-Dfile.encoding=UTF-8",
      "-Dclient.encoding.override=UTF-8",
      "-Dde.dytanic.cloudnet.wrapper.relocate.io.netty.packagePrefix=de.dytanic.cloudnet.wrapper.relocate.",
      "-DIReallyKnowWhatIAmDoingISwear=true",
      "-Dcloudnet.wrapper.messages.language=" + LanguageManager.getLanguage()
    ));

    ServiceEnvironmentType type = this.getServiceConfiguration().getProcessConfig().getEnvironment();
    Path applicationFile = this.selectApplicationFile(type);
    String applicationMainClass = type.getMainClass(applicationFile);

    if (applicationFile == null || applicationMainClass == null) {
      CloudNetDriver.getInstance().getLogger()
        .error(LanguageManager.getMessage("cloud-service-jar-file-not-found-error")
          .replace("%task%", this.getServiceId().getTaskName())
          .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
          .replace("%id%", this.getServiceId().getUniqueId().toString()));

      ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider()
        .getServiceTask(this.getServiceId().getTaskName());
      if (serviceTask != null) {
        serviceTask.forbidServiceStarting(SERVICE_ERROR_RESTART_DELAY * 1000);
      }

      this.stop();
      return;
    }

    Path wrapperFile = Paths.get(System.getProperty("cloudnet.tempDir", "temp"), "caches", "wrapper.jar");

    commandArguments.addAll(this.getServiceConfiguration().getProcessConfig().getJvmOptions());
    commandArguments.addAll(Arrays.asList(
      "-Xmx" + this.getServiceConfiguration().getProcessConfig().getMaxHeapMemorySize() + "M",
      "-cp", type.getClasspath(wrapperFile, applicationFile),
      "-javaagent:" + wrapperFile.toAbsolutePath()
    ));

    try (JarInputStream stream = new JarInputStream(Files.newInputStream(wrapperFile))) {
      commandArguments.add(stream.getManifest().getMainAttributes().getValue("Main-Class"));
    }

    // internal process arguments
    commandArguments.add(applicationMainClass);
    commandArguments.add(applicationFile.toAbsolutePath().toString());
    commandArguments.add(Boolean.toString(type.shouldPreloadClassesBeforeStartup(applicationFile)));

    this.postConfigureServiceEnvironmentStartParameters(commandArguments);

    commandArguments.addAll(this.getServiceConfiguration().getProcessConfig().getProcessParameters());

    this.process = new ProcessBuilder()
      .command(commandArguments)
      .directory(this.getDirectoryPath().toFile())
      .start();
  }

  @Nullable
  private Integer stop0(boolean force) {
    if (this.lifeCycle == ServiceLifeCycle.RUNNING) {
      if (!super.preStop()) {
        return null;
      }

      int exitValue = this.stopProcess(force);

      if (this.getNetworkChannel() != null) {
        try {
          this.getNetworkChannel().close();
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }

      super.postStop(exitValue);
      return exitValue;
    }

    return -1;
  }

  private int stopProcess(boolean force) {
    if (this.process != null) {

      if (this.process.isAlive()) {
        try {
          OutputStream outputStream = this.process.getOutputStream();
          outputStream.write("stop\n".getBytes());
          outputStream.flush();
          outputStream.write("end\n".getBytes());
          outputStream.flush();

          if (this.process
            .waitFor(CloudNet.getInstance().getConfig().getProcessTerminationTimeoutSeconds(), TimeUnit.SECONDS)) {
            return this.process.exitValue();
          }
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }

      if (!force) {
        this.process.destroy();
      } else {
        this.process.destroyForcibly();
      }

      try {
        return this.process.exitValue();
      } catch (Throwable ignored) {
        try {
          this.process.destroyForcibly();

          return this.process.exitValue();
        } catch (Exception ignored0) {
          return -1;
        }
      }
    }

    return -1;
  }

  private void delete0(boolean sendUpdate) {
    if (this.lifeCycle == ServiceLifeCycle.DELETED) {
      return;
    }

    if (this.lifeCycle == ServiceLifeCycle.RUNNING && this.stop0(true) == null) { // User cancelled stop event
      return;
    }

    super.deleteFiles(sendUpdate);
  }

  @NotNull
  public DefaultServiceConsoleLogCache getServiceConsoleLogCache() {
    return this.serviceConsoleLogCache;
  }

  public Process getProcess() {
    return this.process;
  }

  @Nullable
  private Path selectApplicationFile(ServiceEnvironmentType serviceEnvironmentType) throws IOException {
    Path applicationFile = null;
    List<Path> files = Files.list(this.getDirectoryPath()).collect(Collectors.toList());

    for (ServiceEnvironment environment : serviceEnvironmentType.getEnvironments()) {
      for (Path file : files) {
        String fileName = file.toString().toLowerCase();

        if (fileName.endsWith(".jar") && fileName.contains(environment.getName())) {
          applicationFile = file;
          break;
        }
      }

      if (applicationFile != null) {
        break;
      }
    }

    return applicationFile;
  }
}
