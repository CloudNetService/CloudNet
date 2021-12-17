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

import static de.dytanic.cloudnet.common.language.I18n.trans;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.andThen;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.bool;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.nonEmptyStr;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.uuid;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.validatedHostAndPort;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.console.animation.setup.ConsoleSetupAnimation;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionAnswerType;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionListEntry.Builder;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class DefaultClusterSetup implements DefaultSetup {

  @Override
  public void applyQuestions(@NotNull ConsoleSetupAnimation animation) {
    animation.addEntries(
      QuestionListEntry.<Boolean>builder()
        .key("installCluster")
        .translatedQuestion("cloudnet-init-setup-cluster-install")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .parser(bool())
          .recommendation("no")
          .possibleResults("yes", "no")
          .addResultListener((__, result) -> {
            // check if we need to apply further questions
            if (result) {
              animation.addEntriesFirst(
                // node name
                QuestionListEntry.<String>builder()
                  .key("nodeId")
                  .translatedQuestion("cloudnet-init-setup-node-id")
                  .answerType(QuestionAnswerType.<String>builder()
                    .parser(nonEmptyStr())
                    .recommendation("Node-1"))
                  .build(),
                // cluster unique id
                QuestionListEntry.<UUID>builder()
                  .key("clusterId")
                  .translatedQuestion("cloudnet-init-setup-cluster-cluster-id")
                  .answerType(QuestionAnswerType.<UUID>builder()
                    .parser(uuid())
                    .recommendation(UUID.randomUUID())
                  )
                  .build(),
                // other node names
                QuestionListEntry.<Collection<String>>builder()
                  .key("nodesList")
                  .translatedQuestion("cloudnet-init-setup-cluster-list-nodes")
                  .answerType(QuestionAnswerType.<Collection<String>>builder()
                    .parser(andThen(nonEmptyStr(), val -> Arrays.asList(val.split(";"))))
                    // add a question for each host of each node in the new cluster
                    .addResultListener((___, nodes) -> animation.addEntriesFirst(nodes.stream()
                      .map(node -> QuestionListEntry.<HostAndPort>builder()
                        .key("nodeHost-" + node)
                        .question(trans("cloudnet-init-setup-cluster-node-host").replace("%node%", node))
                        .answerType(QuestionAnswerType.<HostAndPort>builder()
                          .parser(validatedHostAndPort(true))))
                      .map(Builder::build)
                      .toArray(QuestionListEntry[]::new))))
                  .build());
            }
          }))
        .build());
  }

  @Override
  public void handleResults(@NotNull ConsoleSetupAnimation animation) {
    if (animation.getResult("installCluster")) {
      var config = CloudNet.getInstance().getConfig();

      // apply the cluster settings
      Collection<String> nodeNames = animation.getResult("nodesList");
      config.setClusterConfig(new NetworkCluster(
        animation.getResult("clusterId"),
        nodeNames.stream()
          .map(node -> {
            HostAndPort address = animation.getResult("nodeHost-" + node);
            // whitelist the address
            config.getIpWhitelist().add(address.host());
            // map to a node
            return new NetworkClusterNode(node, new HostAndPort[]{address});
          })
          .collect(Collectors.toList())));
      // save the config to apply all changes
      config.save();
    }
  }
}
