package de.dytanic.cloudnet.ext.signs;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationTaskEntry;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractSignManagement {

    private static final Comparator<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>>
            ENTRY_COMPARATOR = new ServiceInfoSnapshotEntryComparator(),
            ENTRY_COMPARATOR_2 = new ServiceInfoSnapshotEntryComparator2();

    private static AbstractSignManagement instance;
    private final AtomicInteger[] indexes = new AtomicInteger[]{
            new AtomicInteger(-1), //starting
            new AtomicInteger(-1) //search
    };
    private final Map<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> services = new ConcurrentHashMap<>();
    protected Set<Sign> signs;

    public AbstractSignManagement() {
        instance = this;

        this.signs = new HashSet<>();
        this.signs.addAll(this.getSignsFromNode());

        // fetching the already existing services and making them available for the signs, if matching
        Wrapper.getInstance().getCloudServiceProvider().getCloudServices().stream()
                .filter(this::isMatchingCloudService)
                .forEach(serviceInfoSnapshot -> this.putService(serviceInfoSnapshot, entry -> this.fromServiceInfoSnapshot(serviceInfoSnapshot, entry), false));
    }

    public static AbstractSignManagement getInstance() {
        return instance;
    }

    protected abstract void updateSignNext(@NotNull Sign sign, @NotNull SignLayout signLayout, @Nullable ServiceInfoSnapshot serviceInfoSnapshot);

    /**
     * Removes all signs that don't exist anymore
     */
    public abstract void cleanup();

    /**
     * Runs a task on the main thread of the current application
     *
     * @param runnable the task
     * @param delay    the delay the task should have
     */
    protected abstract void runTaskLater(@NotNull Runnable runnable, long delay);

    private void putService(ServiceInfoSnapshot serviceInfoSnapshot, Function<SignConfigurationEntry, ServiceInfoState> stateFunction) {
        this.putService(serviceInfoSnapshot, stateFunction, true);
    }

    private void putService(ServiceInfoSnapshot serviceInfoSnapshot, Function<SignConfigurationEntry, ServiceInfoState> stateFunction, boolean updateSigns) {
        if (!this.isMatchingCloudService(serviceInfoSnapshot)) {
            return;
        }

        SignConfigurationEntry entry = this.getOwnSignConfigurationEntry();
        if (entry == null) {
            return;
        }

        this.services.put(serviceInfoSnapshot.getServiceId().getUniqueId(), new Pair<>(serviceInfoSnapshot, stateFunction.apply(entry)));
        if (updateSigns) {
            this.updateSigns();
        }
    }

    public void onRegisterService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> ServiceInfoState.STOPPED);
    }

    public void onStartService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> ServiceInfoState.STARTING);
    }

    public void onConnectService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> this.fromServiceInfoSnapshot(serviceInfoSnapshot, entry));
    }

    public void onUpdateServiceInfo(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> this.fromServiceInfoSnapshot(serviceInfoSnapshot, entry));
    }

    public void onDisconnectService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> ServiceInfoState.STOPPED);
    }

    public void onStopService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> ServiceInfoState.STOPPED);
    }

    public void onUnregisterService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        if (!this.isMatchingCloudService(serviceInfoSnapshot)) {
            return;
        }

        SignConfigurationEntry entry = this.getOwnSignConfigurationEntry();
        if (entry == null) {
            return;
        }

        this.services.remove(serviceInfoSnapshot.getServiceId().getUniqueId());
        this.updateSigns();
    }

    public void onSignAdd(@NotNull Sign sign) {
        this.signs.add(sign);
        CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
    }

    public void onSignRemove(@NotNull Sign sign) {
        this.signs.stream()
                .filter(filterSign -> filterSign.getSignId() == sign.getSignId())
                .findFirst().ifPresent(signEntry -> this.signs.remove(signEntry));

        CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
    }

    public void updateSigns() {
        SignConfigurationEntry signConfiguration = this.getOwnSignConfigurationEntry();
        if (signConfiguration == null) {
            return;
        }

        List<Sign> signs = this.signs.stream()
                .filter(sign -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(sign.getProvidedGroup()))
                .collect(Collectors.toList());

        if (signs.isEmpty()) {
            return;
        }

        List<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>> cachedFilter = new ArrayList<>();
        List<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>> entries = this.services.entrySet().stream()
                .filter(item -> item.getValue().getSecond() != ServiceInfoState.STOPPED).sorted(ENTRY_COMPARATOR).collect(Collectors.toList());

        for (Sign sign : signs) {
            this.updateSign(sign, signConfiguration, cachedFilter, entries);
        }
    }

    private void updateSign(Sign sign,
                            SignConfigurationEntry signConfiguration,
                            List<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>> cachedFilter,
                            List<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>> entries) {

        cachedFilter.addAll(entries.stream().filter(entry -> {
            boolean access = Arrays.asList(entry.getValue().getFirst().getConfiguration().getGroups()).contains(sign.getTargetGroup());

            if (sign.getTemplatePath() != null) {
                boolean condition = false;

                for (ServiceTemplate template : entry.getValue().getFirst().getConfiguration().getTemplates()) {
                    if (sign.getTemplatePath().equals(template.getTemplatePath())) {
                        condition = true;
                        break;
                    }
                }

                access = condition;
            }

            return access;
        }).collect(Collectors.toList()));

        cachedFilter.sort(ENTRY_COMPARATOR_2);

        if (!cachedFilter.isEmpty()) {
            Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> entry = cachedFilter.get(0);

            sign.setServiceInfoSnapshot(entry.getValue().getFirst());

            this.applyState(sign, signConfiguration, entry.getValue().getFirst(), entry.getValue().getSecond());

            entries.remove(entry);

        } else {
            sign.setServiceInfoSnapshot(null);

            if (!signConfiguration.getSearchLayouts().getSignLayouts().isEmpty()) {
                this.updateSignNext(sign, signConfiguration.getSearchLayouts().getSignLayouts().get(this.indexes[1].get()), null);
            }
        }

        cachedFilter.clear();
    }

    private void applyState(Sign sign, SignConfigurationEntry signConfiguration, ServiceInfoSnapshot serviceInfoSnapshot, ServiceInfoState state) {
        switch (state) {
            case STOPPED: {
                sign.setServiceInfoSnapshot(null);

                if (!signConfiguration.getSearchLayouts().getSignLayouts().isEmpty()) {
                    this.updateSignNext(sign, signConfiguration.getSearchLayouts().getSignLayouts().get(this.indexes[1].get()), null);
                }
            }
            break;
            case STARTING: {
                sign.setServiceInfoSnapshot(null);

                if (!signConfiguration.getStartingLayouts().getSignLayouts().isEmpty()) {
                    this.updateSignNext(sign, signConfiguration.getStartingLayouts().getSignLayouts().get(this.indexes[0].get()), serviceInfoSnapshot);
                }
            }
            break;
            case EMPTY_ONLINE: {
                SignLayout signLayout = null;

                SignConfigurationTaskEntry taskEntry = this.getValidSignConfigurationTaskEntryFromSignConfigurationEntry(signConfiguration, sign.getTargetGroup());

                if (taskEntry != null) {
                    signLayout = taskEntry.getEmptyLayout();
                }

                if (signLayout == null) {
                    signLayout = signConfiguration.getDefaultEmptyLayout();
                }

                this.updateSignNext(sign, signLayout, serviceInfoSnapshot);
            }
            break;
            case ONLINE: {
                SignLayout signLayout = null;

                SignConfigurationTaskEntry taskEntry = this.getValidSignConfigurationTaskEntryFromSignConfigurationEntry(signConfiguration, sign.getTargetGroup());

                if (taskEntry != null) {
                    signLayout = taskEntry.getOnlineLayout();
                }

                if (signLayout == null) {
                    signLayout = signConfiguration.getDefaultOnlineLayout();
                }

                this.updateSignNext(sign, signLayout, serviceInfoSnapshot);
            }
            break;
            case FULL_ONLINE: {
                SignLayout signLayout = null;

                SignConfigurationTaskEntry taskEntry = this.getValidSignConfigurationTaskEntryFromSignConfigurationEntry(signConfiguration, sign.getTargetGroup());

                if (taskEntry != null) {
                    signLayout = taskEntry.getFullLayout();
                }

                if (signLayout == null) {
                    signLayout = signConfiguration.getDefaultFullLayout();
                }

                this.updateSignNext(sign, signLayout, serviceInfoSnapshot);
            }
            break;
        }
    }

    protected String addDataToLine(@NotNull Sign sign, @NotNull String input, @Nullable ServiceInfoSnapshot serviceInfoSnapshot) {
        if (serviceInfoSnapshot == null) {
            return input;
        }

        input = input.replace("%task%", serviceInfoSnapshot.getServiceId().getTaskName());
        input = input.replace("%task_id%", String.valueOf(serviceInfoSnapshot.getServiceId().getTaskServiceId()));
        input = input.replace("%group%", sign.getTargetGroup());
        input = input.replace("%name%", serviceInfoSnapshot.getServiceId().getName());
        input = input.replace("%uuid%", serviceInfoSnapshot.getServiceId().getUniqueId().toString().split("-")[0]);
        input = input.replace("%node%", serviceInfoSnapshot.getServiceId().getNodeUniqueId());
        input = input.replace("%environment%", String.valueOf(serviceInfoSnapshot.getServiceId().getEnvironment()));
        input = input.replace("%life_cycle%", String.valueOf(serviceInfoSnapshot.getLifeCycle()));
        input = input.replace("%runtime%", serviceInfoSnapshot.getConfiguration().getRuntime());
        input = input.replace("%port%", String.valueOf(serviceInfoSnapshot.getConfiguration().getPort()));
        input = input.replace("%cpu_usage%", CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(serviceInfoSnapshot.getProcessSnapshot().getCpuUsage()));
        input = input.replace("%threads%", String.valueOf(serviceInfoSnapshot.getProcessSnapshot().getThreads().size()));


        input = input.replace("%online%",
                (serviceInfoSnapshot.getProperties().contains("Online") && serviceInfoSnapshot.getProperties().getBoolean("Online")
                        ? "Online" : "Offline"
                ));
        input = input.replace("%online_players%", String.valueOf(serviceInfoSnapshot.getProperties().getInt("Online-Count")));
        input = input.replace("%max_players%", String.valueOf(serviceInfoSnapshot.getProperties().getInt("Max-Players")));
        input = input.replace("%motd%", serviceInfoSnapshot.getProperties().getString("Motd", ""));
        input = input.replace("%extra%", serviceInfoSnapshot.getProperties().getString("Extra", ""));
        input = input.replace("%state%", serviceInfoSnapshot.getProperties().getString("State", ""));
        input = input.replace("%version%", serviceInfoSnapshot.getProperties().getString("Version", ""));
        input = input.replace("%whitelist%", (serviceInfoSnapshot.getProperties().contains("Whitelist-Enabled") &&
                serviceInfoSnapshot.getProperties().getBoolean("Whitelist-Enabled")
                ? "Enabled" : "Disabled"
        ));

        return input;
    }

    private SignConfigurationTaskEntry getValidSignConfigurationTaskEntryFromSignConfigurationEntry(SignConfigurationEntry entry, String targetTask) {
        return entry.getTaskLayouts().stream()
                .filter(signConfigurationTaskEntry -> signConfigurationTaskEntry.getTask() != null &&
                        signConfigurationTaskEntry.getEmptyLayout() != null &&
                        signConfigurationTaskEntry.getFullLayout() != null &&
                        signConfigurationTaskEntry.getOnlineLayout() != null &&
                        signConfigurationTaskEntry.getTask().equalsIgnoreCase(targetTask))
                .findFirst()
                .orElse(null);
    }

    public SignConfigurationEntry getOwnSignConfigurationEntry() {
        return SignConfigurationProvider.load().getConfigurations().stream()
                .filter(signConfigurationEntry -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(signConfigurationEntry.getTargetGroup()))
                .findFirst()
                .orElse(null);
    }

    private boolean isEmptyService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.isConnected() &&
                serviceInfoSnapshot.getProperties().getBoolean("Online") &&
                serviceInfoSnapshot.getProperties().contains("Online-Count") &&
                serviceInfoSnapshot.getProperties().getInt("Online-Count") == 0;
    }

    private boolean isFullService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.isConnected() &&
                serviceInfoSnapshot.getProperties().getBoolean("Online") &&
                serviceInfoSnapshot.getProperties().contains("Online-Count") &&
                serviceInfoSnapshot.getProperties().contains("Max-Players") &&
                serviceInfoSnapshot.getProperties().getInt("Online-Count") >=
                        serviceInfoSnapshot.getProperties().getInt("Max-Players");
    }

    private boolean isStartingService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING && !serviceInfoSnapshot.getProperties().contains("Online");
    }

    private boolean isIngameService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING
                && serviceInfoSnapshot.isConnected()
                &&
                serviceInfoSnapshot.getProperties().getBoolean("Online")
                && (
                (serviceInfoSnapshot.getProperties().contains("Motd") &&
                        (
                                serviceInfoSnapshot.getProperties().getString("Motd").toLowerCase().contains("ingame") ||
                                        serviceInfoSnapshot.getProperties().getString("Motd").toLowerCase().contains("running")
                        )
                ) ||
                        (serviceInfoSnapshot.getProperties().contains("Extra") &&
                                (
                                        serviceInfoSnapshot.getProperties().getString("Extra").toLowerCase().contains("ingame") ||
                                                serviceInfoSnapshot.getProperties().getString("Extra").toLowerCase().contains("running")
                                )
                        ) ||
                        (serviceInfoSnapshot.getProperties().contains("State") &&
                                (
                                        serviceInfoSnapshot.getProperties().getString("State").toLowerCase().contains("ingame") ||
                                                serviceInfoSnapshot.getProperties().getString("State").toLowerCase().contains("running")
                                )
                        )
        );
    }

    private ServiceInfoState fromServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot, SignConfigurationEntry signConfiguration) {
        if (serviceInfoSnapshot.getLifeCycle() != ServiceLifeCycle.RUNNING || this.isIngameService(serviceInfoSnapshot)) {
            return ServiceInfoState.STOPPED;
        }

        if (this.isEmptyService(serviceInfoSnapshot)) {
            return ServiceInfoState.EMPTY_ONLINE;
        }

        if (this.isFullService(serviceInfoSnapshot)) {
            if (!signConfiguration.isSwitchToSearchingWhenServiceIsFull()) {
                return ServiceInfoState.FULL_ONLINE;
            } else {
                return ServiceInfoState.STOPPED;
            }
        }

        if (this.isStartingService(serviceInfoSnapshot)) {
            return ServiceInfoState.STARTING;
        }

        if (serviceInfoSnapshot.isConnected() &&
                serviceInfoSnapshot.getProperties().getBoolean("Online")) {
            return ServiceInfoState.ONLINE;
        } else {
            return ServiceInfoState.STOPPED;
        }
    }

    public void sendSignAddUpdate(@NotNull Sign sign) {
        CloudNetDriver.getInstance().getMessenger()
                .sendChannelMessage(
                        SignConstants.SIGN_CHANNEL_NAME,
                        SignConstants.SIGN_CHANNEL_ADD_SIGN_MESSAGE,
                        new JsonDocument("sign", sign)
                );
    }

    public void sendSignRemoveUpdate(@NotNull Sign sign) {
        CloudNetDriver.getInstance().getMessenger()
                .sendChannelMessage(
                        SignConstants.SIGN_CHANNEL_NAME,
                        SignConstants.SIGN_CHANNEL_REMOVE_SIGN_MESSAGE,
                        new JsonDocument("sign", sign)
                );
    }

    protected Collection<Sign> getSignsFromNode() {
        ITask<Collection<Sign>> signs = CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacket(
                CloudNetDriver.getInstance().getNetworkClient().getChannels().iterator().next(),
                SignConstants.SIGN_CHANNEL_SYNC_CHANNEL_PROPERTY,
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, SignConstants.SIGN_CHANNEL_SYNC_ID_GET_SIGNS_COLLECTION_PROPERTY),
                new byte[0],
                documentPair -> documentPair.getFirst().get("signs", SignConstants.COLLECTION_SIGNS)
        );

        try {
            return signs.get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    public void updateSignConfiguration(@NotNull SignConfiguration signConfiguration) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                SignConstants.SIGN_CHANNEL_NAME,
                SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION,
                new JsonDocument("signConfiguration", signConfiguration)
        );
    }

    private boolean isMatchingCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {

        if (serviceInfoSnapshot != null) {

            ServiceEnvironmentType currentEnvironment = Wrapper.getInstance().getServiceId().getEnvironment();
            ServiceEnvironmentType serviceEnvironment = serviceInfoSnapshot.getServiceId().getEnvironment();

            return (serviceEnvironment.isMinecraftJavaServer() && currentEnvironment.isMinecraftJavaServer())
                    || (serviceEnvironment.isMinecraftBedrockServer() && currentEnvironment.isMinecraftBedrockServer());
        }

        return false;
    }

    protected void executeStartingTask() {
        SignConfigurationEntry signConfigurationEntry = this.getOwnSignConfigurationEntry();
        AtomicInteger startingIndex = indexes[0];

        if (signConfigurationEntry != null && signConfigurationEntry.getStartingLayouts() != null &&
                signConfigurationEntry.getStartingLayouts().getSignLayouts().size() > 0) {
            if (startingIndex.get() == -1) {
                startingIndex.set(0);
            }

            if ((startingIndex.get() + 1) < signConfigurationEntry.getStartingLayouts().getSignLayouts().size()) {
                startingIndex.incrementAndGet();
            } else {
                startingIndex.set(0);
            }

            this.runTaskLater(
                    this::executeStartingTask,
                    20 / (Math.min(signConfigurationEntry.getStartingLayouts().getAnimationsPerSecond(), 20))
            );
        } else {
            startingIndex.set(-1);
            this.runTaskLater(this::executeStartingTask, 20);
        }

        CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
    }

    protected void executeSearchingTask() {
        SignConfigurationEntry signConfigurationEntry = this.getOwnSignConfigurationEntry();
        AtomicInteger searchingIndex = this.indexes[1];

        if (signConfigurationEntry != null && signConfigurationEntry.getSearchLayouts() != null &&
                signConfigurationEntry.getSearchLayouts().getSignLayouts().size() > 0) {
            if (searchingIndex.get() == -1) {
                searchingIndex.set(0);
            }

            if ((searchingIndex.get() + 1) < signConfigurationEntry.getSearchLayouts().getSignLayouts().size()) {
                searchingIndex.incrementAndGet();
            } else {
                searchingIndex.set(0);
            }

            this.runTaskLater(
                    this::executeSearchingTask,
                    20 / (Math.min(signConfigurationEntry.getSearchLayouts().getAnimationsPerSecond(), 20))
            );
        } else {
            searchingIndex.set(-1);
            this.runTaskLater(this::executeSearchingTask, 20);
        }

        CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
    }

    public Map<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> getServices() {
        return this.services;
    }

    public AtomicInteger[] getIndexes() {
        return this.indexes;
    }

    public Set<Sign> getSigns() {
        return this.signs;
    }

    public void setSigns(@NotNull Set<Sign> signs) {
        this.signs = signs;
    }

    private enum ServiceInfoState {
        STOPPED(0),
        STARTING(1),
        EMPTY_ONLINE(2),
        ONLINE(3),
        FULL_ONLINE(4);

        private final int value;

        ServiceInfoState(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    private static final class ServiceInfoSnapshotEntryComparator implements Comparator<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>> {

        @Override
        public int compare(Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> o1, Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> o2) {
            return o1.getValue().getFirst().getServiceId().getName().compareTo(o2.getValue().getFirst().getServiceId().getName());
        }
    }

    private static final class ServiceInfoSnapshotEntryComparator2 implements Comparator<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>> {

        @Override
        public int compare(Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> o1, Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> o2) {
            return o1.getValue().getSecond().getValue() + o2.getValue().getSecond().getValue();
        }
    }

}