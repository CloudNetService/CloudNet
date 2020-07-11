package de.dytanic.cloudnet.setup;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeHostAndPort;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeInt;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultConfigSetup implements DefaultSetup {

    private List<String> internalIPs;
    private String preferredIP;

    private List<String> detectAllIPAddresses() throws SocketException {
        List<String> resultAddresses = new ArrayList<>();

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                String address = addresses.nextElement().getHostAddress();
                if (!resultAddresses.contains(address)) {
                    resultAddresses.add(address);
                }
            }
        }

        return resultAddresses;
    }

    private String detectPreferredIP(List<String> internalIPs) {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception exception) {
            return internalIPs.get(0);
        }
    }

    @Override
    public void applyQuestions(ConsoleQuestionListAnimation animation) throws Exception {
        this.internalIPs = this.detectAllIPAddresses();
        this.internalIPs.add("127.0.0.1");
        this.internalIPs.add("127.0.1.1");

        this.preferredIP = this.detectPreferredIP(this.internalIPs);

        animation.addEntry(new QuestionListEntry<>(
                "eula",
                LanguageManager.getMessage("cloudnet-init-eula"),
                new QuestionAnswerTypeBoolean() {
                    @Override
                    public boolean isValidInput(String input) {
                        return input.equalsIgnoreCase(super.getTrueString());
                    }

                    @Override
                    public String getInvalidInputMessage(String input) {
                        return LanguageManager.getMessage("cloudnet-init-eula-not-accepted");
                    }
                }
        ));


        animation.addEntry(new QuestionListEntry<>(
                "internalHost",
                LanguageManager.getMessage("cloudnet-init-setup-internal-host"),
                new QuestionAnswerTypeHostAndPort() {
                    @Override
                    public String getRecommendation() {
                        return DefaultConfigSetup.this.preferredIP + ":1410";
                    }

                    @Override
                    public List<String> getCompletableAnswers() {
                        return DefaultConfigSetup.this.internalIPs.stream()
                                .map(internalIP -> internalIP + ":1410")
                                .collect(Collectors.toList());
                    }
                }
        ));

        animation.addEntry(new QuestionListEntry<>(
                "webHost",
                LanguageManager.getMessage("cloudnet-init-setup-web-host"),
                new QuestionAnswerTypeHostAndPort() {
                    @Override
                    public String getRecommendation() {
                        return DefaultConfigSetup.this.preferredIP + ":2812";
                    }

                    @Override
                    public List<String> getCompletableAnswers() {
                        return DefaultConfigSetup.this.internalIPs.stream()
                                .map(internalIP -> internalIP + ":2812")
                                .collect(Collectors.toList());
                    }
                }
        ));

        animation.addEntry(new QuestionListEntry<>(
                "memory",
                LanguageManager.getMessage("cloudnet-init-setup-memory"),
                new QuestionAnswerTypeInt() {
                    @Override
                    public boolean isValidInput(String input) {
                        return super.isValidInput(input) && Integer.parseInt(input) >= 0;
                    }

                    @Override
                    public String getRecommendation() {
                        long systemMaxMemory = (CPUUsageResolver.getSystemMemory() / 1048576);
                        return String.valueOf((int) (systemMaxMemory - Math.min(systemMaxMemory, 2048)));
                    }

                    @Override
                    public List<String> getCompletableAnswers() {
                        long systemMaxMemory = (CPUUsageResolver.getSystemMemory() / 1048576);
                        return Arrays.stream(new int[]{2048, 4096, 8192, 16384, 32768, 65536, (int) (systemMaxMemory - Math.min(systemMaxMemory, 2048))})
                                .filter(value -> value < systemMaxMemory && value > 0)
                                .mapToObj(String::valueOf)
                                .sorted(Collections.reverseOrder())
                                .collect(Collectors.toList());
                    }
                }
        ));
    }

    @Override
    public void execute(ConsoleQuestionListAnimation animation) {
        if (animation.hasResult("internalHost")) {
            HostAndPort defaultHost = (HostAndPort) animation.getResult("internalHost");

            CloudNet.getInstance().getConfig().setHostAddress(defaultHost.getHost());
            CloudNet.getInstance().getConfig().setIdentity(new NetworkClusterNode(
                    animation.hasResult("nodeId") ? (String) animation.getResult("nodeId") : "Node-1",
                    new HostAndPort[]{defaultHost}
            ));

            CloudNet.getInstance().getConfig().getIpWhitelist().addAll(this.internalIPs);
            if (!this.internalIPs.contains(defaultHost.getHost())) {
                CloudNet.getInstance().getConfig().getIpWhitelist().add(defaultHost.getHost());
            }
        }

        if (animation.hasResult("webHost")) {
            CloudNet.getInstance().getConfig().setHttpListeners(new ArrayList<>(Collections.singletonList(
                    (HostAndPort) animation.getResult("webHost")
            )));
        }

        if (animation.hasResult("memory")) {
            CloudNet.getInstance().getConfig().setMaxMemory((int) animation.getResult("memory"));
        }
    }

    @Override
    public boolean shouldAsk(boolean configFileAvailable) {
        return !configFileAvailable;
    }
}
