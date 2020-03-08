package de.dytanic.cloudnet.setup;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeHostAndPort;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeInt;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeString;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionProvider;
import de.dytanic.cloudnet.template.install.ServiceVersionType;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DefaultInstallation {

    private CloudNet cloudNet;

    private DefaultTaskSetup taskSetup = new DefaultTaskSetup();

    private ConsoleQuestionListAnimation animation;

    public DefaultInstallation(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

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

    private ConsoleQuestionListAnimation animation() {
        return this.animation != null ? this.animation : (this.animation = new ConsoleQuestionListAnimation(
                "DefaultInstallation",
                null,
                () -> "&f   ___  _                    _ &b     __    __  _____  &3  _____              _           _  _ \n" +
                        "&f  / __\\| |  ___   _   _   __| |&b  /\\ \\ \\  /__\\/__   \\ &3  \\_   \\ _ __   ___ | |_   __ _ | || |\n" +
                        "&f / /   | | / _ \\ | | | | / _` |&b /  \\/ / /_\\    / /\\/ &3   / /\\/| '_ \\ / __|| __| / _` || || |\n" +
                        "&f/ /___ | || (_) || |_| || (_| |&b/ /\\  / //__   / /    &3/\\/ /_  | | | |\\__ \\| |_ | (_| || || |\n" +
                        "&f\\____/ |_| \\___/  \\__,_| \\__,_|&b\\_\\ \\/  \\__/   \\/     &3\\____/  |_| |_||___/ \\__| \\__,_||_||_|\n" +
                        "&f                               &b                      &3                                      ",
                () -> null,
                "&r> &e"
        ));
    }

    public void executeFirstStartSetup(IConsole console, boolean configFileAvailable) throws SocketException {
        List<String> internalIPs = this.detectAllIPAddresses();
        internalIPs.add("127.0.0.1");
        internalIPs.add("127.0.1.1");

        String preferredIP = this.detectPreferredIP(internalIPs);

        if (!configFileAvailable) {
            this.animation().addEntry(new QuestionListEntry<>(
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


            this.animation().addEntry(new QuestionListEntry<>(
                    "nodeId",
                    LanguageManager.getMessage("cloudnet-init-setup-node-id"),
                    new QuestionAnswerTypeString() {
                        @Override
                        public String getRecommendation() {
                            return "Node-1";
                        }
                    }
            ));

            this.animation().addEntry(new QuestionListEntry<>(
                    "internalHost",
                    LanguageManager.getMessage("cloudnet-init-setup-internal-host"),
                    new QuestionAnswerTypeHostAndPort() {
                        @Override
                        public String getRecommendation() {
                            return preferredIP + ":1410";
                        }

                        @Override
                        public List<String> getCompletableAnswers() {
                            return internalIPs.stream()
                                    .map(internalIP -> internalIP + ":1410")
                                    .collect(Collectors.toList());
                        }
                    }
            ));

            this.animation().addEntry(new QuestionListEntry<>(
                    "webHost",
                    LanguageManager.getMessage("cloudnet-init-setup-web-host"),
                    new QuestionAnswerTypeHostAndPort() {
                        @Override
                        public String getRecommendation() {
                            return preferredIP + ":2812";
                        }

                        @Override
                        public List<String> getCompletableAnswers() {
                            return internalIPs.stream()
                                    .map(internalIP -> internalIP + ":2812")
                                    .collect(Collectors.toList());
                        }
                    }
            ));

            this.animation().addEntry(new QuestionListEntry<>(
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

        if (!this.cloudNet.getCloudServiceManager().isFileCreated()) {
            this.taskSetup.applyTaskQuestions(this.animation());
        }

        if (this.animation != null) {
            this.animation.setCancellable(false);

            console.clearScreen();
            console.startAnimation(this.animation);

            ITask<Void> task = new ListenableTask<>(() -> null);

            this.animation.addFinishHandler(() -> {

                if (this.animation.hasResult("internalHost")) {
                    HostAndPort defaultHost = (HostAndPort) this.animation.getResult("internalHost");

                    this.cloudNet.getConfig().setHostAddress(defaultHost.getHost());
                    this.cloudNet.getConfig().setIdentity(new NetworkClusterNode(
                            animation.hasResult("nodeId") ? (String) animation.getResult("nodeId") : "Node-" + UUID.randomUUID().toString().split("-")[0],
                            new HostAndPort[]{defaultHost}
                    ));

                    this.cloudNet.getConfig().getIpWhitelist().addAll(internalIPs);
                    if (!internalIPs.contains(defaultHost.getHost())) {
                        this.cloudNet.getConfig().getIpWhitelist().add(defaultHost.getHost());
                    }
                }

                if (this.animation.hasResult("webHost")) {
                    this.cloudNet.getConfig().setHttpListeners(new ArrayList<>(Collections.singletonList(
                            (HostAndPort) this.animation.getResult("webHost")
                    )));
                }

                if (this.animation.hasResult("memory")) {
                    this.cloudNet.getConfig().setMaxMemory((int) this.animation.getResult("memory"));
                }

                try {
                    task.call();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            });

            try {
                task.get(); //wait for the results by the user
            } catch (InterruptedException | ExecutionException exception) {
                exception.printStackTrace();
            }

        }
    }

    public void initDefaultTasks() {
        this.taskSetup.execute(this.animation);
    }

    public void initDefaultPermissionGroups() {
        if (this.cloudNet.getPermissionManagement().getGroups().isEmpty() && System.getProperty("cloudnet.default.permissions.skip") == null) {
            IPermissionGroup adminPermissionGroup = new PermissionGroup("Admin", 100);
            adminPermissionGroup.addPermission("*");
            adminPermissionGroup.addPermission("Proxy", "*");
            adminPermissionGroup.setPrefix("&4Admin &8| &7");
            adminPermissionGroup.setColor("&7");
            adminPermissionGroup.setSuffix("&f");
            adminPermissionGroup.setDisplay("&4");
            adminPermissionGroup.setSortId(10);

            this.cloudNet.getPermissionManagement().addGroup(adminPermissionGroup);

            IPermissionGroup defaultPermissionGroup = new PermissionGroup("default", 100);
            defaultPermissionGroup.addPermission("bukkit.broadcast.user", true);
            defaultPermissionGroup.setDefaultGroup(true);
            defaultPermissionGroup.setPrefix("&7");
            defaultPermissionGroup.setColor("&7");
            defaultPermissionGroup.setSuffix("&f");
            defaultPermissionGroup.setDisplay("&7");
            defaultPermissionGroup.setSortId(10);

            this.cloudNet.getPermissionManagement().addGroup(defaultPermissionGroup);
        }
    }

}
