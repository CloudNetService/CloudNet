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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeCollection;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeString;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeUUID;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeValidHostAndPort;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import java.util.Collection;
import java.util.UUID;

public class DefaultClusterSetup implements DefaultSetup {

  @Override
  public void applyQuestions(ConsoleQuestionListAnimation animation) {
    animation.addEntry(new QuestionListEntry<>(
      "installCluster",
      LanguageManager.getMessage("cloudnet-init-setup-cluster-install"),
      new QuestionAnswerTypeBoolean() {
        @Override
        public String getRecommendation() {
          return super.getFalseString();
        }
      }
    ));

    animation.addEntryCompletionListener((entry, result) -> {
      if (entry.getKey().equals("installCluster") && (boolean) result) {
        animation.addEntriesFirst(
          new QuestionListEntry<>(
            "nodeId",
            LanguageManager.getMessage("cloudnet-init-setup-node-id"),
            new QuestionAnswerTypeString() {
              @Override
              public String getRecommendation() {
                return "Node-1";
              }
            }
          ),
          new QuestionListEntry<>(
            "clusterId",
            LanguageManager.getMessage("cloudnet-init-setup-cluster-cluster-id"),
            new QuestionAnswerTypeUUID()
          ),
          new QuestionListEntry<>(
            "nodesList",
            LanguageManager.getMessage("cloudnet-init-setup-cluster-list-nodes"),
            new QuestionAnswerTypeCollection().disallowEmpty()
          )
        );
      }
    });

    animation.addEntryCompletionListener((entry, result) -> {
      if (entry.getKey().equals("nodesList")) {
        Collection<String> nodes = (Collection<String>) animation.getResult("nodesList");

        animation.addEntriesFirst(nodes.stream()
          .map(node -> new QuestionListEntry<>(
            "nodeHost-" + node,
            LanguageManager.getMessage("cloudnet-init-setup-cluster-node-host").replace("%node%", node),
            new QuestionAnswerTypeValidHostAndPort()
          ))
          .toArray(QuestionListEntry[]::new)
        );
      }
    });
  }

  @Override
  public void execute(ConsoleQuestionListAnimation animation) {
    if (animation.hasResult("clusterId")) {
      UUID clusterId = (UUID) animation.getResult("clusterId");

      CloudNet.getInstance().getConfig().getClusterConfig().setClusterId(clusterId);
      CloudNet.getInstance().getConfig().save();
    }

    if (animation.hasResult("nodesList")) {
      Collection<String> nodes = (Collection<String>) animation.getResult("nodesList");
      for (String node : nodes) {
        HostAndPort host = (HostAndPort) animation.getResult("nodeHost-" + node);
        CloudNet.getInstance().getConfig().getClusterConfig().getNodes()
          .add(new NetworkClusterNode(node, new HostAndPort[]{host}));
        CloudNet.getInstance().getConfig().getIpWhitelist().add(host.getHost());
      }
      CloudNet.getInstance().getConfig().save();
    }
  }

  @Override
  public boolean shouldAsk(boolean configFileAvailable) {
    return !configFileAvailable;
  }

}
