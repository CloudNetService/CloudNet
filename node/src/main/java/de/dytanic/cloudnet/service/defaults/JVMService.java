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

import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.function.ThrowableFunction;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.event.EventManager;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceLogEntryEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.event.service.CloudServicePostProcessStartEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreProcessStartEvent;
import de.dytanic.cloudnet.service.CloudServiceManager;
import de.dytanic.cloudnet.service.ServiceConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.log.ProcessServiceLogCache;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class JVMService extends AbstractService {

  protected static final Logger LOGGER = LogManager.logger(JVMService.class);
  protected static final Pattern FILE_NUMBER_PATTERN = Pattern.compile("(\\d+).*");
  protected static final Path WRAPPER_TEMP_FILE = FileUtils.TEMP_DIR.resolve("caches").resolve("wrapper.jar");
  protected static final Collection<String> DEFAULT_JVM_SYSTEM_PROPERTIES = Arrays.asList(
    "-Dfile.encoding=UTF-8",
    "-Dclient.encoding.override=UTF-8",
    "-DIReallyKnowWhatIAmDoingISwear=true",
    "-Djline.terminal=jline.UnsupportedTerminal",
    "-Dlog4j2.formatMsgNoLookups=true");

  protected volatile Process process;

  public JVMService(
    @NonNull ServiceConfiguration configuration,
    @NonNull CloudServiceManager manager,
    @NonNull EventManager eventManager,
    @NonNull CloudNet nodeInstance,
    @NonNull ServiceConfigurationPreparer serviceConfigurationPreparer
  ) {
    super(configuration, manager, eventManager, nodeInstance, serviceConfigurationPreparer);
    super.logCache = new ProcessServiceLogCache(() -> this.process, nodeInstance, this);

    this.initLogHandler();
  }

  @Override
  protected void startProcess() {
    this.eventManager.callEvent(new CloudServicePreProcessStartEvent(this));
    var environmentType = this.serviceConfiguration().serviceId().environment();
    // load the wrapper information if possible
    var wrapperInformation = this.prepareWrapperFile();
    if (wrapperInformation == null) {
      LOGGER.severe("Unable to load wrapper information for service startup");
      return;
    }
    // load the application file information if possible
    var applicationInformation = this.prepareApplicationFile(environmentType);
    if (applicationInformation == null) {
      LOGGER.severe("Unable to load application information for service startup");
      return;
    }

    // get the agent class of the application (if any)
    var agentClass = applicationInformation.second().mainAttributes().getValue("Premain-Class");
    if (agentClass == null) {
      // some old versions named the agent class 'Launcher-Agent-Class' - try that
      agentClass = applicationInformation.second().mainAttributes().getValue("Launcher-Agent-Class");
    }

    // prepare the service startup
    List<String> arguments = new ArrayList<>();

    // add the java command to start the service
    var overriddenJavaCommand = this.serviceConfiguration().javaCommand();
    arguments.add(overriddenJavaCommand == null ? this.nodeConfiguration().javaCommand() : overriddenJavaCommand);
    // add the jvm flags of the service configuration
    arguments.addAll(this.serviceConfiguration().processConfig().jvmOptions());

    // set the maximum heap memory setting. Xms matching Xmx because if not there is unused memory
    arguments.add("-Xmx" + this.serviceConfiguration().processConfig().maxHeapMemorySize() + "M");
    arguments.add("-Xms" + this.serviceConfiguration().processConfig().maxHeapMemorySize() + "M");

    // override some default configuration options
    arguments.addAll(DEFAULT_JVM_SYSTEM_PROPERTIES);
    arguments.add("-javaagent:" + wrapperInformation.first().toAbsolutePath());
    arguments.add("-Dcloudnet.wrapper.messages.language=" + I18n.language());

    // add the class path and the main class of the wrapper
    arguments.add("-cp");
    arguments.add(wrapperInformation.first().toAbsolutePath().toString());
    arguments.add(wrapperInformation.second().getValue("Main-Class")); // the main class we want to invoke first

    // add all internal process parameters (they will be removed by the wrapper before starting the application)
    arguments.add(applicationInformation.second().mainAttributes().getValue("Main-Class"));
    arguments.add(String.valueOf(agentClass)); // the agent class might be null
    arguments.add(applicationInformation.first().toAbsolutePath().toString());
    arguments.add(Boolean.toString(applicationInformation.second().preloadJarContent()));

    // add all process parameters
    arguments.addAll(environmentType.defaultProcessArguments());
    arguments.addAll(this.serviceConfiguration().processConfig().processParameters());

    // try to start the process like that
    this.doStartProcess(arguments, wrapperInformation.first(), applicationInformation.first());
  }

  @Override
  protected void stopProcess() {
    if (this.process != null) {
      // try to send a shutdown command
      this.runCommand("end");
      this.runCommand("stop");

      try {
        // wait until the process termination seconds exceeded
        if (this.process.waitFor(this.nodeConfiguration().processTerminationTimeoutSeconds(), TimeUnit.SECONDS)) {
          this.process.exitValue(); // validation that the process terminated
          this.process = null; // reset as there is no fall-through
          return;
        }
      } catch (IllegalThreadStateException | InterruptedException ignored) { // force shutdown the process
      }
      // force destroy the process now - not much we can do here more than that
      this.process.toHandle().destroyForcibly();
      this.process = null;
    }
  }

  @Override
  public void runCommand(@NonNull String command) {
    if (this.process != null) {
      try {
        var out = this.process.getOutputStream();
        // write & flush
        out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
      } catch (IOException exception) {
        LOGGER.finer("Unable to dispatch command %s on service %s", exception, command, this.serviceId());
      }
    }
  }

  @Override
  public @NonNull String runtime() {
    return "jvm";
  }

  @Override
  public boolean alive() {
    return this.process != null && this.process.toHandle().isAlive();
  }

  protected void doStartProcess(
    @NonNull List<String> arguments,
    @NonNull Path wrapperPath,
    @NonNull Path applicationFilePath
  ) {
    try {
      this.process = new ProcessBuilder(arguments).directory(this.serviceDirectory.toFile()).start();
      this.eventManager.callEvent(new CloudServicePostProcessStartEvent(this, this.process.toHandle()));
    } catch (IOException exception) {
      LOGGER.severe("Unable to start process in %s with command line %s",
        exception,
        this.serviceDirectory,
        String.join(" ", arguments));
    }
  }

  protected void initLogHandler() {
    super.logCache.addHandler(($, line) -> {
      for (var logTarget : super.logTargets.entrySet()) {
        if (logTarget.getKey().equals(ChannelMessageSender.self().toTarget())) {
          this.eventManager.callEvent(logTarget.getValue(), new CloudServiceLogEntryEvent(this.lastServiceInfo, line));
        } else {
          ChannelMessage.builder()
            .target(logTarget.getKey())
            .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
            .message("screen_new_line")
            .buffer(DataBuf.empty()
              .writeObject(this.lastServiceInfo)
              .writeString(logTarget.getValue())
              .writeString(line))
            .build()
            .send();
        }
      }
    });
  }

  protected @Nullable Pair<Path, Attributes> prepareWrapperFile() {
    // check if the wrapper file is there - unpack it if not
    if (Files.notExists(WRAPPER_TEMP_FILE)) {
      FileUtils.createDirectory(WRAPPER_TEMP_FILE.getParent());
      try (var stream = JVMService.class.getClassLoader().getResourceAsStream("wrapper.jar")) {
        // ensure that the wrapper file is there
        if (stream == null) {
          throw new IllegalStateException("Build-in \"wrapper.jar\" missing, unable to start jvm based services");
        }
        // copy the wrapper file to the output directory
        Files.copy(stream, WRAPPER_TEMP_FILE, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException exception) {
        LOGGER.severe("Unable to copy \"wrapper.jar\" to %s", exception, WRAPPER_TEMP_FILE);
      }
    }
    // read the main class
    return this.completeJarAttributeInformation(
      WRAPPER_TEMP_FILE,
      file -> file.getManifest().getMainAttributes());
  }

  protected @Nullable Pair<Path, ApplicationStartupInformation> prepareApplicationFile(
    @NonNull ServiceEnvironmentType environmentType
  ) {
    // collect all names of environment names
    var environments = this.nodeInstance.serviceVersionProvider().serviceVersionTypes().values().stream()
      .filter(environment -> environment.environmentType().equals(environmentType.name()))
      .map(ServiceEnvironment::name)
      .collect(Collectors.collectingAndThen(Collectors.toSet(), result -> {
        // add a default fallback value which applied to all environments
        result.add("application");
        return result;
      }));

    try {
      // walk the file tree and filter the best application file
      return Files.walk(this.serviceDirectory, 1)
        .filter(path -> {
          var filename = path.getFileName().toString();
          // check if the file is a jar file - it must end with '.jar' for that
          if (!filename.endsWith(".jar")) {
            return false;
          }
          // search if any environment is in the name of the file
          for (var environment : environments) {
            if (filename.contains(environment)) {
              return true;
            }
          }
          // not an application file for the environment
          return false;
        }).min((left, right) -> {
          // get the first number from the left path
          var leftMatcher = FILE_NUMBER_PATTERN.matcher(left.getFileName().toString());
          // no match -> neutral
          if (!leftMatcher.matches()) {
            return 0;
          }

          // get the first number from the right patch
          var rightMatcher = FILE_NUMBER_PATTERN.matcher(right.getFileName().toString());
          // no match -> neutral
          if (!rightMatcher.matches()) {
            return 0;
          }

          // extract the numbers
          var leftNumber = Ints.tryParse(leftMatcher.group(1));
          var rightNumber = Ints.tryParse(rightMatcher.group(1));
          // compare both of the numbers
          return leftNumber == null || rightNumber == null ? 0 : Integer.compare(leftNumber, rightNumber);
        })
        .map(path -> this.completeJarAttributeInformation(
          path,
          file -> new ApplicationStartupInformation(
            file.getEntry("META-INF/versions.list") != null,
            file.getManifest().getMainAttributes())
        )).orElse(null);
    } catch (IOException exception) {
      LOGGER.severe("Unable to find application file information in %s for environment %s",
        exception,
        this.serviceDirectory,
        environmentType);
      return null;
    }
  }

  protected @Nullable <T> Pair<Path, T> completeJarAttributeInformation(
    @NonNull Path jarFilePath,
    @NonNull ThrowableFunction<JarFile, T, IOException> mapper
  ) {
    // open the file and lookup the main class
    try (var jarFile = new JarFile(jarFilePath.toFile())) {
      return new Pair<>(jarFilePath, mapper.apply(jarFile));
    } catch (IOException exception) {
      LOGGER.severe("Unable to open wrapper file at %s for reading: ", exception, jarFilePath);
      return null;
    }
  }

  protected record ApplicationStartupInformation(boolean preloadJarContent, @NonNull Attributes mainAttributes) {

  }
}
