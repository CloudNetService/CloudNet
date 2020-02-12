package de.dytanic.cloudnet;

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

    private final Map<String, Runnable> defaultTaskInstallations = new HashMap<>();

    private String selectedDefaultTasksInstallation;

    public DefaultInstallation(CloudNet cloudNet) {
        this.cloudNet = cloudNet;

        this.defaultTaskInstallations.put("nothing", () -> this.cloudNet.getCloudServiceManager().removeAllPermanentServiceTasks());
        this.defaultTaskInstallations.put("recommended", this::installRecommended);
        this.defaultTaskInstallations.put("bedrock", this::installBedrock);
        this.defaultTaskInstallations.put("java-bungee-1.12.2", () -> this.installJavaBungee("1.12.2"));
        this.defaultTaskInstallations.put("java-bungee-1.13.2", () -> this.installJavaBungee("1.13.2"));
        this.defaultTaskInstallations.put("java-bungee-1.14.4", () -> this.installJavaBungee("1.14.4"));
        this.defaultTaskInstallations.put("java-bungee-1.15.2", () -> this.installJavaBungee("1.15.2"));
        this.defaultTaskInstallations.put("java-velocity-1.12.2", () -> this.installJavaVelocity("1.12.2"));
        this.defaultTaskInstallations.put("java-velocity-1.13.2", () -> this.installJavaVelocity("1.13.2"));
        this.defaultTaskInstallations.put("java-velocity-1.14.4", () -> this.installJavaVelocity("1.14.4"));
        this.defaultTaskInstallations.put("java-velocity-1.15.2", () -> this.installJavaVelocity("1.15.2"));
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

    public void executeFirstStartSetup(IConsole console, boolean configFileAvailable) throws SocketException {
        Collection<QuestionListEntry<?>> entries = new ArrayList<>();

        List<String> internalIPs = this.detectAllIPAddresses();
        internalIPs.add("127.0.0.1");
        internalIPs.add("127.0.1.1");

        String preferredIP = this.detectPreferredIP(internalIPs);

        if (!configFileAvailable) {
            entries.add(new QuestionListEntry<>(
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


            entries.add(new QuestionListEntry<>(
                    "nodeId",
                    LanguageManager.getMessage("cloudnet-init-setup-node-id"),
                    new QuestionAnswerTypeString() {
                        @Override
                        public String getRecommendation() {
                            return "Node-1";
                        }
                    }
            ));

            entries.add(new QuestionListEntry<>(
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

            entries.add(new QuestionListEntry<>(
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

            entries.add(new QuestionListEntry<>(
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
            this.applyTaskQuestions(entries);
        }

        if (!entries.isEmpty()) {
            ConsoleQuestionListAnimation animation = new ConsoleQuestionListAnimation(
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
            );

            for (QuestionListEntry<?> entry : entries) {
                animation.addEntry(entry);
            }

            animation.setCancellable(false);

            console.clearScreen();
            console.startAnimation(animation);

            ITask<Void> task = new ListenableTask<>(() -> null);

            animation.addFinishHandler(() -> {

                if (animation.hasResult("tasksInstallation")) {
                    this.selectedDefaultTasksInstallation = (String) animation.getResult("tasksInstallation");
                }

                if (animation.hasResult("internalHost")) {
                    HostAndPort defaultHost = (HostAndPort) animation.getResult("internalHost");

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

                if (animation.hasResult("webHost")) {
                    this.cloudNet.getConfig().setHttpListeners(new ArrayList<>(Collections.singletonList(
                            (HostAndPort) animation.getResult("webHost")
                    )));
                }

                if (animation.hasResult("memory")) {
                    this.cloudNet.getConfig().setMaxMemory((int) animation.getResult("memory"));
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

    private void applyTaskQuestions(Collection<QuestionListEntry<?>> entries) {
        entries.add(new QuestionListEntry<>(
                "tasksInstallation",
                LanguageManager.getMessage("cloudnet-init-setup-tasks-default-installation"),
                new QuestionAnswerTypeString() {
                    @Override
                    public String getRecommendation() {
                        return "recommended";
                    }

                    @Override
                    public boolean isValidInput(String input) {
                        return super.isValidInput(input) &&
                                !input.trim().isEmpty() &&
                                this.getPossibleAnswers().stream().anyMatch(possibleAnswer -> possibleAnswer.equalsIgnoreCase(input));
                    }

                    @Override
                    public String getPossibleAnswersAsString() {
                        return System.lineSeparator() + String.join(System.lineSeparator(), this.getPossibleAnswers());
                    }

                    @Override
                    public Collection<String> getPossibleAnswers() {
                        return this.getCompletableAnswers();
                    }

                    @Override
                    public List<String> getCompletableAnswers() {
                        return new ArrayList<>(DefaultInstallation.this.defaultTaskInstallations.keySet());
                    }
                }
        ));
    }

    public void initDefaultTasks() {
        if (this.selectedDefaultTasksInstallation != null) {
            this.defaultTaskInstallations.get(this.selectedDefaultTasksInstallation).run();
        }
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

    private void installRecommended() {
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create Proxy bungeecord");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create Lobby minecraft_server");

        //Create groups
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "groups create Global-Server");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "groups create Global-Proxy");

        //Add groups
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Proxy add group Global-Proxy");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Lobby add group Global-Server");

        //Install
        this.installServerVersions(
                new PreparedTemplate(
                        new ServiceTemplate("Global", "proxy", "local"),
                        "bungeecord", "latest",
                        this.cloudNet.getServiceVersionProvider()
                ),
                new PreparedTemplate(
                        new ServiceTemplate("Global", "bukkit", "local"),
                        "paperspigot", "latest",
                        this.cloudNet.getServiceVersionProvider()
                )
        );

        //Add templates
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "groups group Global-Server add template local:Global/bukkit");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "groups group Global-Proxy add template local:Global/proxy");

        //Set configurations
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Proxy set minServiceCount 1");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Lobby set minServiceCount 1");
    }

    private void installBedrock() {
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create Proxy waterdog");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create Lobby nukkit");

        this.installServerVersions(
                new PreparedTemplate(
                        new ServiceTemplate("Proxy", "default", "local"),
                        "waterdog", "latest",
                        this.cloudNet.getServiceVersionProvider()
                ),
                new PreparedTemplate(
                        new ServiceTemplate("Lobby", "default", "local"),
                        "nukkit", "latest",
                        this.cloudNet.getServiceVersionProvider()
                )
        );

        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Proxy set minServiceCount 1");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Lobby set minServiceCount 1");
    }

    private void installJavaBungee(String spigotVersion) {
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create Proxy bungeecord");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create Lobby minecraft_server");

        this.installServerVersions(
                new PreparedTemplate(
                        new ServiceTemplate("Proxy", "default", "local"),
                        "bungeecord", "latest",
                        this.cloudNet.getServiceVersionProvider()
                ),
                new PreparedTemplate(
                        new ServiceTemplate("Lobby", "default", "local"),
                        "paperspigot", spigotVersion,
                        this.cloudNet.getServiceVersionProvider()
                )
        );

        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Proxy set minServiceCount 1");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Lobby set minServiceCount 1");
    }

    private void installJavaVelocity(String spigotVersion) {
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create Proxy velocity");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks create Lobby minecraft_server");

        this.installServerVersions(
                new PreparedTemplate(
                        new ServiceTemplate("Proxy", "default", "local"),
                        "velocity", "latest",
                        this.cloudNet.getServiceVersionProvider()
                ),
                new PreparedTemplate(
                        new ServiceTemplate("Lobby", "default", "local"),
                        "paperspigot", spigotVersion,
                        this.cloudNet.getServiceVersionProvider()
                )
        );

        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Proxy set minServiceCount 1");
        this.cloudNet.getCommandMap().dispatchCommand(this.cloudNet.getConsoleCommandSender(), "tasks task Lobby set minServiceCount 1");
    }

    private void installServerVersions(PreparedTemplate... templates) {
        for (PreparedTemplate template : templates) {
            template.prepareTemplate(this.cloudNet);
        }
    }

    private static final class PreparedTemplate {
        private ServiceTemplate template;
        private ServiceVersionType versionType;
        private ServiceVersion version;

        public PreparedTemplate(ServiceTemplate template, ServiceVersionType versionType, ServiceVersion version) {
            this.template = template;
            this.versionType = versionType;
            this.version = version;
        }

        public PreparedTemplate(ServiceTemplate template, String versionTypeName, String versionName, ServiceVersionProvider versionProvider) {
            this.template = template;
            this.versionType = versionProvider.getServiceVersionType(versionTypeName).get();
            this.version = this.versionType.getVersion(versionName).get();
        }

        public void prepareTemplate(CloudNet cloudNet) {
            ITemplateStorage templateStorage = cloudNet.getServicesRegistry().getService(ITemplateStorage.class, this.template.getStorage());
            cloudNet.getServiceVersionProvider().installServiceVersion(this.versionType, this.version, templateStorage, template);
        }
    }

}
