package de.dytanic.cloudnet.setup;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.*;
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
                CloudNet.getInstance().getConfig().getClusterConfig().getNodes().add(new NetworkClusterNode(node, new HostAndPort[]{host}));
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
