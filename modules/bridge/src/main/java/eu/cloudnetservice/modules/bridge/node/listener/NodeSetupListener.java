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

package eu.cloudnetservice.modules.bridge.node.listener;

import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.config.ProxyFallbackConfiguration;
import eu.cloudnetservice.node.console.animation.setup.answer.Parsers;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionAnswerType;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionListEntry;
import eu.cloudnetservice.node.event.setup.SetupCompleteEvent;
import eu.cloudnetservice.node.event.setup.SetupInitiateEvent;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class NodeSetupListener {

  private final QuestionListEntry<String> bridgeFallbackQuestionEntry;

  @Inject
  public NodeSetupListener(@NonNull ServiceTaskProvider taskProvider, @NonNull ServiceVersionProvider versionProvider) {
    // create the bridge fallback question entry
    this.bridgeFallbackQuestionEntry = this.createBridgeFallbackEntry(taskProvider, versionProvider);
  }

  @EventListener
  public void handleSetupInitialize(@NonNull SetupInitiateEvent event) {
    event.setup().entries().stream()
      .filter(entry -> entry.key().equals("taskEnvironment"))
      .findFirst()
      .ifPresent(entry -> entry.answerType().thenAccept(($, env) -> {
        // only add an entry for minecraft proxies
        if (!event.setup().hasResult("generateBridgeFallback")
          && ServiceEnvironmentType.minecraftProxy((ServiceEnvironmentType) env)) {
          event.setup().addEntries(this.bridgeFallbackQuestionEntry);
        }
      }));
  }

  @EventListener
  public void handleSetupComplete(@NonNull SetupCompleteEvent event, @NonNull BridgeManagement bridgeManagement) {
    String fallbackName = event.setup().result("generateBridgeFallback");
    // check if we want to add a fallback
    if (fallbackName != null && !fallbackName.isEmpty()) {
      var config = bridgeManagement.configuration();
      config.fallbackConfigurations().add(ProxyFallbackConfiguration.builder()
        .targetGroup(event.setup().result("taskName"))
        .defaultFallbackTask(fallbackName)
        .build());
      bridgeManagement.configuration(config);
    }
  }

  private @NonNull QuestionListEntry<String> createBridgeFallbackEntry(
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull ServiceVersionProvider versionProvider
  ) {
    return QuestionListEntry.<String>builder()
      .key("generateBridgeFallback")
      .translatedQuestion("module-bridge-tasks-setup-default-fallback")
      .answerType(QuestionAnswerType.<String>builder()
        .parser(input -> {
          // we allow an empty input or an existing task
          if (!input.isEmpty() && taskProvider.serviceTask(input) == null) {
            throw Parsers.ParserException.INSTANCE;
          }
          return input;
        })
        .possibleResults(taskProvider.serviceTasks().stream().filter(
            task -> {
              var env = versionProvider.environmentType(task.processConfiguration().environment());
              // only minecraft servers are allowed to be a fallback
              return env != null && ServiceEnvironmentType.minecraftServer(env);
            })
          .map(Named::name)
          .toList()))
      .build();
  }

}
