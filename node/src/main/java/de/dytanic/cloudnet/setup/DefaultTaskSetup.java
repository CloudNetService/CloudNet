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

package de.dytanic.cloudnet.setup;

import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.bool;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.javaVersion;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.serviceEnvironmentType;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.serviceVersion;
import static de.dytanic.cloudnet.driver.service.ServiceEnvironmentType.JAVA_PROXY;
import static de.dytanic.cloudnet.driver.service.ServiceEnvironmentType.JAVA_SERVER;
import static de.dytanic.cloudnet.driver.service.ServiceEnvironmentType.PE_PROXY;
import static de.dytanic.cloudnet.driver.service.ServiceEnvironmentType.PE_SERVER;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.Document;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.console.animation.setup.ConsoleSetupAnimation;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionAnswerType;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionListEntry;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.InstallInformation;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionProvider;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.NonNull;

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
          .parser(bool())
          .addResultListener(($, result) -> {
            if (result) {
              animation.addEntriesFirst(
                // environment
                QuestionListEntry.<ServiceEnvironmentType>builder()
                  .key("proxyEnvironment")
                  .translatedQuestion("cloudnet-init-setup-tasks-proxy-environment")
                  .answerType(QuestionAnswerType.<ServiceEnvironmentType>builder()
                    .parser(serviceEnvironmentType())
                    .possibleResults(this.versionProvider().knownEnvironments().values().stream()
                      .filter(type -> {
                        Document<?> properties = type.properties();
                        return JAVA_PROXY.get(properties) || PE_PROXY.get(properties);
                      })
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
                    .parser(javaVersion()))
                  .build(),
                // proxy service version
                QuestionListEntry.<Pair<ServiceVersionType, ServiceVersion>>builder()
                  .key("proxyVersion")
                  .translatedQuestion("cloudnet-init-setup-tasks-proxy-version")
                  .answerType(QuestionAnswerType.<Pair<ServiceVersionType, ServiceVersion>>builder()
                    .possibleResults(() -> this.completableServiceVersions(
                      animation.result("proxyEnvironment"),
                      animation.result("proxyJavaCommand")))
                    .parser(serviceVersion()))
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
          .parser(bool())
          .addResultListener(($, result) -> {
            if (result) {
              animation.addEntriesFirst(
                // environment
                QuestionListEntry.<ServiceEnvironmentType>builder()
                  .key("serverEnvironment")
                  .translatedQuestion("cloudnet-init-setup-tasks-server-environment")
                  .answerType(QuestionAnswerType.<ServiceEnvironmentType>builder()
                    .parser(serviceEnvironmentType())
                    .possibleResults(this.versionProvider().knownEnvironments().values().stream()
                      .filter(type -> {
                        Document<?> properties = type.properties();
                        return JAVA_SERVER.get(properties) || PE_SERVER.get(properties);
                      })
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
                    .parser(javaVersion()))
                  .build(),
                // server service version
                QuestionListEntry.<Pair<ServiceVersionType, ServiceVersion>>builder()
                  .key("serverVersion")
                  .translatedQuestion("cloudnet-init-setup-tasks-server-version")
                  .answerType(QuestionAnswerType.<Pair<ServiceVersionType, ServiceVersion>>builder()
                    .possibleResults(() -> this.completableServiceVersions(
                      animation.result("serverEnvironment"),
                      animation.result("serverJavaCommand")))
                    .parser(serviceVersion()))
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
    CloudNet.instance().serviceTaskProvider().addPermanentServiceTask(ServiceTask.builder()
      .name(taskName)
      .minServiceCount(1)
      .autoDeleteOnStop(true)
      .maxHeapMemory(maxHeapMemory)
      .javaCommand(javaCommand.first())
      .serviceEnvironmentType(environment)
      .groups(Collections.singletonList(groupName))
      .startPort(environment.defaultStartPort())
      .templates(Collections.singletonList(template))
      .build());

    // create the global group template
    var groupTemplate = ServiceTemplate.builder().prefix(GLOBAL_TEMPLATE_PREFIX).name(groupName).build();
    this.initializeTemplate(groupTemplate, environment, false);
    // build the new global group
    var groupConfiguration = GroupConfiguration.builder()
      .name(groupName)
      .addTargetEnvironment(environment.name())
      .addTemplate(groupTemplate);

    // check if we are executing the step for the "Global-Server" group
    if (GLOBAL_SERVER_GROUP_NAME.equals(groupName)) {
      // add the aikar flags for the "Global-Server" group
      groupConfiguration.jvmOptions(AIKAR_FLAGS);
    }
    // register the group
    CloudNet.instance().groupConfigurationProvider().addGroupConfiguration(groupConfiguration.build());
    // create a group specifically for the task
    CloudNet.instance().groupConfigurationProvider().addGroupConfiguration(GroupConfiguration.builder()
      .name(taskName)
      .addTemplate(template)
      .build());

    // install the service template
    this.initializeTemplate(template, environment, true);
    // check if the user chose to install a version
    if (version != null) {
      CloudNet.instance().serviceVersionProvider().installServiceVersion(InstallInformation.builder()
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
      TemplateStorageUtil.createAndPrepareTemplate(template, template.storage(), environment, installDefaultFiles);
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
    return this.versionProvider().serviceVersionTypes().values().stream()
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

  protected @NonNull ServiceVersionProvider versionProvider() {
    return CloudNet.instance().serviceVersionProvider();
  }
}
