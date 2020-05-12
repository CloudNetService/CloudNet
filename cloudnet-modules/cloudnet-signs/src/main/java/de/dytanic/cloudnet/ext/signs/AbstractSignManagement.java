package de.dytanic.cloudnet.ext.signs;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoStateWatcher;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationTaskEntry;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class AbstractSignManagement extends ServiceInfoStateWatcher {

    private static final Comparator<Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState>>
            ENTRY_NAME_COMPARATOR = Comparator.comparing(entry -> entry.getFirst().getName()),
            ENTRY_STATE_COMPARATOR = Comparator.comparingInt(entry -> entry.getSecond().getValue());

    private final AtomicInteger[] indexes = new AtomicInteger[]{
            new AtomicInteger(-1), //starting
            new AtomicInteger(-1) //search
    };
    protected Set<Sign> signs;

    public AbstractSignManagement() {
        Collection<Sign> signsFromNode = this.getSignsFromNode();
        this.signs = signsFromNode == null ? new HashSet<>() : signsFromNode.stream()
                .filter(sign -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(sign.getProvidedGroup()))
                .collect(Collectors.toSet());

        super.includeExistingServices();
    }

    /**
     * @deprecated SignManagement should be accessed via the {@link de.dytanic.cloudnet.common.registry.IServicesRegistry}
     */
    @Deprecated
    public static AbstractSignManagement getInstance() {
        return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(AbstractSignManagement.class);
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


    @Override
    protected void handleUpdate() {
        this.updateSigns();
    }

    @Override
    protected boolean shouldWatchService(ServiceInfoSnapshot serviceInfoSnapshot) {
        if (serviceInfoSnapshot != null) {

            ServiceEnvironmentType currentEnvironment = Wrapper.getInstance().getServiceId().getEnvironment();
            ServiceEnvironmentType serviceEnvironment = serviceInfoSnapshot.getServiceId().getEnvironment();

            return (serviceEnvironment.isMinecraftJavaServer() && currentEnvironment.isMinecraftJavaServer())
                    || (serviceEnvironment.isMinecraftBedrockServer() && currentEnvironment.isMinecraftBedrockServer());
        }

        return false;
    }

    @Override
    protected boolean shouldShowFullServices() {
        return !this.getOwnSignConfigurationEntry().isSwitchToSearchingWhenServiceIsFull();
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equals(SignConstants.SIGN_CHANNEL_NAME)) {
            return;
        }

        switch (event.getMessage().toLowerCase()) {
            case SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION: {
                SignConfiguration signConfiguration = event.getData().get("signConfiguration", SignConfiguration.TYPE);
                SignConfigurationProvider.setLocal(signConfiguration);
            }
            break;
            case SignConstants.SIGN_CHANNEL_ADD_SIGN_MESSAGE: {
                Sign sign = event.getData().get("sign", Sign.TYPE);

                if (sign != null) {
                    this.addSign(sign);
                }
            }
            break;
            case SignConstants.SIGN_CHANNEL_REMOVE_SIGN_MESSAGE: {
                Sign sign = event.getData().get("sign", Sign.TYPE);

                if (sign != null) {
                    this.removeSign(sign);
                }
            }
            break;
        }
    }

    /**
     * Adds a sign to this wrapper instance
     *
     * @param sign the sign to add
     * @return if the sign is allowed to exist on this wrapper instance
     */
    public boolean addSign(@NotNull Sign sign) {
        if (Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(sign.getProvidedGroup())) {
            this.signs.add(sign);
            CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
            return true;
        }
        return false;
    }

    /**
     * Removes a sign from this wrapper instance
     *
     * @param sign the sign to remove
     */
    public void removeSign(@NotNull Sign sign) {
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

        List<Sign> signs = new ArrayList<>(this.signs);
        Collections.sort(signs);

        List<Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState>> entries = super.services.values().stream()
                .filter(pair -> pair.getSecond() != ServiceInfoStateWatcher.ServiceInfoState.STOPPED)
                .sorted(ENTRY_NAME_COMPARATOR)
                .collect(Collectors.toList());

        for (Sign sign : signs) {
            this.updateSign(sign, signConfiguration, entries);
        }
    }

    private void updateSign(Sign sign,
                            SignConfigurationEntry signConfiguration,
                            List<Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState>> entries) {

        Optional<Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState>> optionalEntry = entries.stream()
                .filter(entry -> {
                    boolean access = Arrays.asList(entry.getFirst().getConfiguration().getGroups()).contains(sign.getTargetGroup());

                    if (sign.getTemplatePath() != null) {
                        boolean condition = false;

                        for (ServiceTemplate template : entry.getFirst().getConfiguration().getTemplates()) {
                            if (sign.getTemplatePath().equals(template.getTemplatePath())) {
                                condition = true;
                                break;
                            }
                        }

                        access = condition;
                    }

                    return access;
                })
                .min(ENTRY_STATE_COMPARATOR);

        if (optionalEntry.isPresent()) {
            Pair<ServiceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState> entry = optionalEntry.get();

            sign.setServiceInfoSnapshot(entry.getFirst());
            this.applyState(sign, signConfiguration, entry.getFirst(), entry.getSecond());

            entries.remove(entry);
        } else {
            sign.setServiceInfoSnapshot(null);

            if (!signConfiguration.getSearchLayouts().getSignLayouts().isEmpty()) {
                this.updateSignNext(sign, signConfiguration.getSearchLayouts().getSignLayouts().get(this.indexes[1].get()), null);
            }
        }
    }

    private void applyState(Sign sign, SignConfigurationEntry signConfiguration, ServiceInfoSnapshot serviceInfoSnapshot, ServiceInfoStateWatcher.ServiceInfoState state) {
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


    /**
     * Adds a sign to the whole cluster and the database
     *
     * @param sign the sign to add
     */
    public void sendSignAddUpdate(@NotNull Sign sign) {
        CloudNetDriver.getInstance().getMessenger()
                .sendChannelMessage(
                        SignConstants.SIGN_CHANNEL_NAME,
                        SignConstants.SIGN_CHANNEL_ADD_SIGN_MESSAGE,
                        new JsonDocument("sign", sign)
                );
    }

    /**
     * Removes a sign from the whole cluster and the database
     *
     * @param sign the sign to remove
     */
    public void sendSignRemoveUpdate(@NotNull Sign sign) {
        CloudNetDriver.getInstance().getMessenger()
                .sendChannelMessage(
                        SignConstants.SIGN_CHANNEL_NAME,
                        SignConstants.SIGN_CHANNEL_REMOVE_SIGN_MESSAGE,
                        new JsonDocument("sign", sign)
                );
    }

    /**
     * Returns all signs contained in the CloudNet sign database
     *
     * @return all signs or null, if an error occurred
     */
    @Nullable
    public Collection<Sign> getSignsFromNode() {
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

    /**
     * Updates the SignConfiguration in the whole cluster
     *
     * @param signConfiguration the new SignConfiguration
     */
    public void updateSignConfiguration(@NotNull SignConfiguration signConfiguration) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                SignConstants.SIGN_CHANNEL_NAME,
                SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION,
                new JsonDocument("signConfiguration", signConfiguration)
        );
    }

    protected void executeStartingTask() {
        SignConfigurationEntry signConfigurationEntry = this.getOwnSignConfigurationEntry();
        AtomicInteger startingIndex = this.indexes[0];

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

    public AtomicInteger[] getIndexes() {
        return this.indexes;
    }

    /**
     * Returns a copy of the signs allowed to exist on this wrapper instance
     * Use {@link AbstractSignManagement#addSign(Sign)} and {@link AbstractSignManagement#removeSign(Sign)} for local modification
     *
     * @return a copy of the signs
     */
    public Set<Sign> getSigns() {
        return new HashSet<>(this.signs);
    }

}