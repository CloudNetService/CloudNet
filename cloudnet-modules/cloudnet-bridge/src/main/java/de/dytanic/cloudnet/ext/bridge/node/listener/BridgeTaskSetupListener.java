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

package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeString;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.setup.SetupCompleteEvent;
import de.dytanic.cloudnet.event.setup.SetupResponseEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.ProxyFallback;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class BridgeTaskSetupListener {

  @EventListener
  public void handleSetupComplete(SetupCompleteEvent event) {
    if (!event.getSetup().getName().equals("TaskSetup")) {
      return;
    }

    if (event.getSetup().hasResult("BridgeDefaultFallback")) {
      String taskName = (String) event.getSetup().getResult("name");

      String defaultFallback = (String) event.getSetup().getResult("BridgeDefaultFallback");

      BridgeConfiguration configuration = CloudNetBridgeModule.getInstance().getBridgeConfiguration();
      Optional<ProxyFallbackConfiguration> optionalFallbackConfiguration = configuration
        .getBungeeFallbackConfigurations().stream()
        .filter(proxyFallbackConfiguration -> proxyFallbackConfiguration.getTargetGroup().equals(taskName))
        .findFirst();
      ProxyFallbackConfiguration fallbackConfiguration = optionalFallbackConfiguration
        .orElse(
          new ProxyFallbackConfiguration(
            taskName, defaultFallback,
            new ArrayList<>(
              Collections.singletonList(
                new ProxyFallback(defaultFallback, null, 1)
              )
            )
          )
        );

      if (optionalFallbackConfiguration.isPresent()) {
        fallbackConfiguration.setDefaultFallbackTask(defaultFallback);
      } else {
        configuration.getBungeeFallbackConfigurations().add(fallbackConfiguration);
      }
      CloudNetBridgeModule.getInstance().writeConfiguration(configuration);
    }
  }

  @EventListener
  public void handleSetupResponse(SetupResponseEvent event) {
    if (!event.getSetup().getName().equals("TaskSetup") || !(event.getResponse() instanceof ServiceEnvironmentType)
      || event.getSetup().hasResult("BridgeDefaultFallback")) {
      return;
    }

    ServiceEnvironmentType environment = (ServiceEnvironmentType) event.getResponse();
    if (!environment.isMinecraftJavaProxy() && !environment.isMinecraftBedrockProxy()) {
      return;
    }

    long possibleTasksCount = CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks().stream()
      .filter(serviceTask -> serviceTask.getProcessConfiguration().getEnvironment().isMinecraftBedrockServer() ||
        serviceTask.getProcessConfiguration().getEnvironment().isMinecraftJavaServer())
      .map(ServiceTask::getName).count();
    if (possibleTasksCount == 0) {
      return;
    }

    event.getSetup().addEntry(
      new QuestionListEntry<>(
        "BridgeDefaultFallback",
        LanguageManager.getMessage("module-bridge-tasks-setup-default-fallback"),
        new QuestionAnswerTypeString() {
          @Override
          public String getRecommendation() {
            ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider().getServiceTask("Lobby");
            return serviceTask != null ? serviceTask.getName() : null;
          }

          @Override
          public Collection<String> getPossibleAnswers() {
            return CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks().stream()
              .filter(
                serviceTask -> serviceTask.getProcessConfiguration().getEnvironment().isMinecraftBedrockServer() ||
                  serviceTask.getProcessConfiguration().getEnvironment().isMinecraftJavaServer())
              .map(ServiceTask::getName)
              .collect(Collectors.toList());
          }

          @Override
          public boolean isValidInput(@NotNull String input) {
            ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(input);
            return serviceTask != null &&
              (serviceTask.getProcessConfiguration().getEnvironment().isMinecraftJavaServer() ||
                serviceTask.getProcessConfiguration().getEnvironment().isMinecraftBedrockServer());
          }

          @Override
          public String getInvalidInputMessage(@NotNull String input) {
            return LanguageManager.getMessage("module-bridge-tasks-setup-fallback-task-not-found");
          }

          @Override
          public List<String> getCompletableAnswers() {
            return new ArrayList<>(this.getPossibleAnswers());
          }
        }
      )
    );
  }

}
