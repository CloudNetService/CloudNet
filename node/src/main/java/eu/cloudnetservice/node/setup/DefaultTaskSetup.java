/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.setup;

import eu.cloudnetservice.common.JavaVersion;
import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.node.console.animation.setup.answer.Parsers;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionAnswerType;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionListEntry;
import eu.cloudnetservice.node.template.TemplateStorageUtil;
import eu.cloudnetservice.node.version.ServiceVersion;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import eu.cloudnetservice.node.version.ServiceVersionType;
import eu.cloudnetservice.node.version.information.TemplateVersionInstaller;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;

@Singleton
public class DefaultTaskSetup implements DefaultSetup {

  // see https://aikar.co/2018/07/02/tuning-the-jvm-g1gc-garbage-collector-flags-for-minecraft/
  protected static final Collection<String> AIKAR_FLAGS = Arrays.asList(
    "-XX:+UseG1GC",
    "-XX:+ParallelRefProcEnabled",
    "-XX:MaxGCPauseMillis=200",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+DisableExplicitGC",
    "-XX:+AlwaysPreTouch",
    "-XX:G1NewSizePercent=30",
    "-XX:G1MaxNewSizePercent=40",
    "-XX:G1HeapRegionSize=8M",
    "-XX:G1ReservePercent=20",
    "-XX:G1HeapWastePercent=5",
    "-XX:G1MixedGCCountTarget=4",
    "-XX:InitiatingHeapOccupancyPercent=15",
    "-XX:G1MixedGCLiveThresholdPercent=90",
    "-XX:G1RSetUpdatingPauseTimePercent=5",
    "-XX:SurvivorRatio=32",
    "-XX:+PerfDisableSharedMem",
    "-XX:MaxTenuringThreshold=1",
    "-Dusing.aikars.flags=https://mcflags.emc.gs",
    "-Daikars.new.flags=true"
  );

  protected static final Logger LOGGER = LogManager.logger(DefaultTaskSetup.class);

  protected static final String PROXY_TASK_NAME = "Proxy";
  protected static final String LOBBY_TASK_NAME = "Lobby";

  protected static final String GLOBAL_TEMPLATE_PREFIX = "Global";
  protected static final String GLOBAL_PROXY_GROUP_NAME = "Global-Proxy";
  protected static final String GLOBAL_SERVER_GROUP_NAME = "Global-Server";

  protected final Parsers parsers;
  protected final TemplateStorageUtil storageUtil;
  protected final ServiceTaskProvider taskProvider;
  protected final GroupConfigurationProvider groupProvider;
  protected final ServiceVersionProvider serviceVersionProvider;

  @Inject
  public DefaultTaskSetup(
    @NonNull Parsers parsers,
    @NonNull TemplateStorageUtil storageUtil,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull GroupConfigurationProvider groupProvider,
    @NonNull ServiceVersionProvider serviceVersionProvider
  ) {
    this.parsers = parsers;
    this.storageUtil = storageUtil;
    this.groupProvider = groupProvider;
    this.taskProvider = taskProvider;
    this.serviceVersionProvider = serviceVersionProvider;
  }

  @Override
  public void applyQuestions(@NonNull ConsoleSetupAnimation animation) {
    animation.addEntries(
      // proxy installation request
      QuestionListEntry.<Boolean>builder()
        .key("installProxy")
        .translatedQuestion("cloudnet-init-setup-tasks-should-install-proxy")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .recommendation("yes")
          .possibleResults("yes", "no")
          .parser(this.parsers.bool())
          .addResultListener(($, result) -> {
            if (result) {
              animation.addEntriesFirst(
                // environment
                QuestionListEntry.<ServiceEnvironmentType>builder()
                  .key("proxyEnvironment")
                  .translatedQuestion("cloudnet-init-setup-tasks-proxy-environment")
                  .answerType(QuestionAnswerType.<ServiceEnvironmentType>builder()
                    .parser(this.parsers.serviceEnvironmentType())
                    .possibleResults(this.serviceVersionProvider.knownEnvironments().values().stream()
                      .filter(ServiceEnvironmentType::minecraftProxy)
                      .map(ServiceEnvironmentType::name)
                      .toList()))
                  .build(),
                // Java command
                QuestionListEntry.<Pair<String, JavaVersion>>builder()
                  .key("proxyJavaCommand")
                  .translatedQuestion("cloudnet-init-setup-tasks-javacommand")
                  .answerType(QuestionAnswerType.<Pair<String, JavaVersion>>builder()
                    .recommendation("java")
                    .possibleResults("java")
                    .parser(this.parsers.javaVersion()))
                  .build(),
                // proxy service version
                QuestionListEntry.<Pair<ServiceVersionType, ServiceVersion>>builder()
                  .key("proxyVersion")
                  .translatedQuestion("cloudnet-init-setup-tasks-proxy-version")
                  .answerType(QuestionAnswerType.<Pair<ServiceVersionType, ServiceVersion>>builder()
                    .possibleResults(() -> this.completableServiceVersions(
                      animation.result("proxyEnvironment"),
                      animation.result("proxyJavaCommand")))
                    .parser(this.parsers.serviceVersion()))
                  .build());
            }
          }))
        .build(),
      // server installation request
      QuestionListEntry.<Boolean>builder()
        .key("installServer")
        .translatedQuestion("cloudnet-init-setup-tasks-should-install-server")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .recommendation("yes")
          .possibleResults("yes", "no")
          .parser(this.parsers.bool())
          .addResultListener(($, result) -> {
            if (result) {
              animation.addEntriesFirst(
                // environment
                QuestionListEntry.<ServiceEnvironmentType>builder()
                  .key("serverEnvironment")
                  .translatedQuestion("cloudnet-init-setup-tasks-server-environment")
                  .answerType(QuestionAnswerType.<ServiceEnvironmentType>builder()
                    .parser(this.parsers.serviceEnvironmentType())
                    .possibleResults(this.serviceVersionProvider.knownEnvironments().values().stream()
                      .filter(ServiceEnvironmentType::minecraftServer)
                      .map(ServiceEnvironmentType::name)
                      .toList()))
                  .build(),
                // Java command
                QuestionListEntry.<Pair<String, JavaVersion>>builder()
                  .key("serverJavaCommand")
                  .translatedQuestion("cloudnet-init-setup-tasks-javacommand")
                  .answerType(QuestionAnswerType.<Pair<String, JavaVersion>>builder()
                    .recommendation("java")
                    .possibleResults("java")
                    .parser(this.parsers.javaVersion()))
                  .build(),
                // server service version
                QuestionListEntry.<Pair<ServiceVersionType, ServiceVersion>>builder()
                  .key("serverVersion")
                  .translatedQuestion("cloudnet-init-setup-tasks-server-version")
                  .answerType(QuestionAnswerType.<Pair<ServiceVersionType, ServiceVersion>>builder()
                    .possibleResults(() -> this.completableServiceVersions(
                      animation.result("serverEnvironment"),
                      animation.result("serverJavaCommand")))
                    .parser(this.parsers.serviceVersion()))
                  .build());
            }
          }))
        .build());
  }

  @Override
  public void handleResults(@NonNull ConsoleSetupAnimation animation) {
    // proxy installation
    if (animation.result("installProxy")) {
      this.executeSetup(animation, "proxy", PROXY_TASK_NAME, GLOBAL_PROXY_GROUP_NAME, 256);
    }
    // server installation
    if (animation.result("installServer")) {
      this.executeSetup(animation, "server", LOBBY_TASK_NAME, GLOBAL_SERVER_GROUP_NAME, 512);
    }
  }

  protected void executeSetup(
    @NonNull ConsoleSetupAnimation animation,
    @NonNull String resultPrefix,
    @NonNull String taskName,
    @NonNull String groupName,
    int maxHeapMemory
  ) {
    // read the responses
    ServiceEnvironmentType environment = animation.result(resultPrefix + "Environment");
    Pair<String, ?> javaCommand = animation.result(resultPrefix + "JavaCommand");
    Pair<ServiceVersionType, ServiceVersion> version = animation.result(resultPrefix + "Version");
    // create the task
    var template = ServiceTemplate.builder().prefix(taskName).name("default").build();
    this.taskProvider.addServiceTask(ServiceTask.builder()
      .name(taskName)
      .minServiceCount(1)
      .autoDeleteOnStop(true)
      .maxHeapMemory(maxHeapMemory)
      .javaCommand(javaCommand.first())
      .serviceEnvironmentType(environment)
      .groups(Set.of(taskName))
      .startPort(environment.defaultStartPort())
      .templates(Collections.singletonList(template))
      .build());

    // create the global group template
    var groupTemplate = ServiceTemplate.builder().prefix(GLOBAL_TEMPLATE_PREFIX).name(groupName).build();
    this.initializeTemplate(groupTemplate, environment, false);
    // build the new global group
    var groupConfiguration = GroupConfiguration.builder()
      .name(groupName)
      .modifyTargetEnvironments(env -> env.add(environment.name()))
      .templates(Set.of(groupTemplate));

    // check if we are executing the step for the "Global-Server" group
    if (GLOBAL_SERVER_GROUP_NAME.equals(groupName)) {
      // add the aikar flags for the "Global-Server" group
      groupConfiguration.jvmOptions(AIKAR_FLAGS);
    }
    // register the group
    this.groupProvider.addGroupConfiguration(groupConfiguration.build());
    // create a group specifically for the task
    this.groupProvider.addGroupConfiguration(GroupConfiguration.builder()
      .name(taskName)
      .templates(Set.of(template))
      .build());

    // install the service template
    this.initializeTemplate(template, environment, true);
    // check if the user chose to install a version
    if (version != null) {
      this.serviceVersionProvider.installServiceVersion(TemplateVersionInstaller.builder()
        .serviceVersion(version.second())
        .serviceVersionType(version.first())
        .toTemplate(template)
        .executable(javaCommand.first())
        .build(), false);
    }
  }

  protected void initializeTemplate(
    @NonNull ServiceTemplate template,
    @NonNull ServiceEnvironmentType environment,
    boolean installDefaultFiles
  ) {
    // install the template
    try {
      this.storageUtil.createAndPrepareTemplate(template, template.storage(), environment, installDefaultFiles);
    } catch (IOException exception) {
      LOGGER.severe("Exception while initializing local template %s with environment %s",
        exception,
        template,
        environment);
    }
  }

  protected @NonNull Collection<String> completableServiceVersions(
    @NonNull ServiceEnvironmentType type,
    @NonNull Pair<String, JavaVersion> javaVersion
  ) {
    return this.serviceVersionProvider.serviceVersionTypes().values().stream()
      .filter(versionType -> versionType.environmentType().equals(type.name()))
      .flatMap(serviceVersionType -> serviceVersionType.versions()
        .stream()
        .filter(version -> version.canRun(javaVersion.second()))
        .map(version -> String.format("%s-%s", serviceVersionType.name(), version.name())))
      .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
        result.add("none");
        return result;
      }));
  }
}
