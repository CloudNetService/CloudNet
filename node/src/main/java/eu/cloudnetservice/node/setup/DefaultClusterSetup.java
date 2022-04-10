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

package eu.cloudnetservice.node.setup;

import static eu.cloudnetservice.node.console.animation.setup.answer.Parsers.andThen;
import static eu.cloudnetservice.node.console.animation.setup.answer.Parsers.bool;
import static eu.cloudnetservice.node.console.animation.setup.answer.Parsers.nonEmptyStr;
import static eu.cloudnetservice.node.console.animation.setup.answer.Parsers.uuid;
import static eu.cloudnetservice.node.console.animation.setup.answer.Parsers.validatedHostAndPort;

import com.google.common.collect.Lists;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.cluster.NetworkCluster;
import eu.cloudnetservice.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionAnswerType;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionListEntry;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionListEntry.Builder;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;

public class DefaultClusterSetup implements DefaultSetup {

  @Override
  public void applyQuestions(@NonNull ConsoleSetupAnimation animation) {
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
                        .translatedQuestion("cloudnet-init-setup-cluster-node-host", node)
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
  public void handleResults(@NonNull ConsoleSetupAnimation animation) {
    if (animation.result("installCluster")) {
      var config = Node.instance().config();

      // apply the cluster settings
      Collection<String> nodeNames = animation.result("nodesList");
      config.clusterConfig(new NetworkCluster(
        animation.result("clusterId"),
        nodeNames.stream()
          .map(node -> {
            HostAndPort address = animation.result("nodeHost-" + node);
            // whitelist the address
            config.ipWhitelist().add(address.host());
            // map to a node
            return new NetworkClusterNode(node, Lists.newArrayList(address));
          })
          .collect(Collectors.toList())));
      // save the config to apply all changes
      config.save();
    }
  }
}
