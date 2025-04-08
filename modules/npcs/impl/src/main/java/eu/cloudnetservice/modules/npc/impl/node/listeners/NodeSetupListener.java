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

package eu.cloudnetservice.modules.npc.impl.node.listeners;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.configuration.NPCConfigurationEntry;
import eu.cloudnetservice.modules.npc.impl.node.NodeNPCManagement;
import eu.cloudnetservice.node.impl.console.animation.setup.answer.Parsers;
import eu.cloudnetservice.node.impl.console.animation.setup.answer.QuestionAnswerType;
import eu.cloudnetservice.node.impl.console.animation.setup.answer.QuestionListEntry;
import eu.cloudnetservice.node.impl.event.setup.SetupCompleteEvent;
import eu.cloudnetservice.node.impl.event.setup.SetupInitiateEvent;
import lombok.NonNull;

public final class NodeSetupListener {

  private final NodeNPCManagement management;
  private final QuestionListEntry<Boolean> createEntryQuestionEntry;

  public NodeSetupListener(@NonNull NodeNPCManagement management, @NonNull Parsers parsers) {
    this.management = management;
    this.createEntryQuestionEntry = QuestionListEntry.<Boolean>builder()
      .key("generateDefaultNPCConfigurationEntry")
      .translatedQuestion("module-npc-tasks-setup-generate-default-config")
      .answerType(QuestionAnswerType.<Boolean>builder()
        .parser(parsers.bool())
        .recommendation("no")
        .possibleResults("yes", "no")
        .build())
      .build();
  }

  @EventListener
  public void handle(@NonNull SetupInitiateEvent event) {
    event.setup().entries().stream()
      .filter(entry -> entry.key().equals("taskEnvironment"))
      .findFirst()
      .ifPresent(entry -> entry.answerType().thenAccept(($, environment) -> {
        if (!event.setup().hasResult("generateDefaultNPCConfigurationEntry")
          && ServiceEnvironmentType.minecraftServer((ServiceEnvironmentType) environment)) {
          event.setup().addEntries(this.createEntryQuestionEntry);
        }
      }));
  }

  @EventListener
  public void handle(@NonNull SetupCompleteEvent event) {
    if (event.setup().hasResult("generateDefaultNPCConfigurationEntry")) {
      String taskName = event.setup().result("taskName");
      Boolean generateNPCConfig = event.setup().result("generateDefaultNPCConfigurationEntry");

      if (taskName != null && generateNPCConfig) {
        var entries = this.management.npcConfiguration().entries();
        if (entries.stream().noneMatch(entry -> entry.targetGroup().equals(taskName))) {
          // add the new entry
          entries.add(NPCConfigurationEntry.builder().targetGroup(taskName).build());
          // update the config
          this.management.npcConfiguration(NPCConfiguration.builder()
            .entries(entries)
            .build());
        }
      }
    }
  }
}
