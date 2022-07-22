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

package eu.cloudnetservice.modules.bridge.node.listener;

import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.config.ProxyFallbackConfiguration;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.console.animation.setup.answer.Parsers;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionAnswerType;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionListEntry;
import eu.cloudnetservice.node.event.setup.SetupCompleteEvent;
import eu.cloudnetservice.node.event.setup.SetupInitiateEvent;
import lombok.NonNull;

public record NodeSetupListener(@NonNull BridgeManagement bridgeManagement) {

  private static final QuestionListEntry<String> CREATE_BRIDGE_FALLBACK = QuestionListEntry.<String>builder()
    .key("generateBridgeFallback")
    .translatedQuestion("module-bridge-tasks-setup-default-fallback")
    .answerType(QuestionAnswerType.<String>builder()
      .parser(input -> {
        // we allow an empty input or an existing task
        if (!input.isEmpty() && Node.instance().serviceTaskProvider().serviceTask(input) == null) {
          throw Parsers.ParserException.INSTANCE;
        }
        return input;
      })
      .possibleResults(Node.instance().serviceTaskProvider().serviceTasks().stream().filter(
          task -> {
            var env = Node.instance().serviceVersionProvider()
              .getEnvironmentType(task.processConfiguration().environment());
            // only minecraft servers are allowed to be a fallback
            return env != null && ServiceEnvironmentType.minecraftServer(env);
          })
        .map(Nameable::name)
        .toList()))
    .build();

  @EventListener
  public void handleSetupInitialize(@NonNull SetupInitiateEvent event) {
    event.setup().entries().stream()
      .filter(entry -> entry.key().equals("taskEnvironment"))
      .findFirst()
      .ifPresent(entry -> entry.answerType().thenAccept(($, env) -> {
        // only add an entry for minecraft proxies
        if (!event.setup().hasResult("generateBridgeFallback")
          && ServiceEnvironmentType.minecraftProxy((ServiceEnvironmentType) env)) {
          event.setup().addEntries(CREATE_BRIDGE_FALLBACK);
        }
      }));
  }

  @EventListener
  public void handleSetupComplete(@NonNull SetupCompleteEvent event) {
    String fallbackName = event.setup().result("generateBridgeFallback");
    // check if we want to add a fallback
    if (fallbackName != null && !fallbackName.isEmpty()) {
      var config = this.bridgeManagement.configuration();
      config.fallbackConfigurations().add(ProxyFallbackConfiguration.builder()
        .targetGroup(event.setup().result("taskName"))
        .defaultFallbackTask(fallbackName)
        .build());
      this.bridgeManagement.configuration(config);
    }
  }
}
