package de.dytanic.cloudnet.ext.signs.bukkit;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.signs.*;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class BukkitSignManagement extends AbstractSignManagement {

    private static final Comparator<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>>
            ENTRY_COMPARATOR = new ServiceInfoSnapshotEntryComparator(),
            ENTRY_COMPARATOR_2 = new ServiceInfoSnapshotEntryComparator2();
    private static BukkitSignManagement instance;
    private final Map<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> services = Maps.newConcurrentHashMap();

    private final BukkitCloudNetSignsPlugin plugin;
    private final AtomicInteger[] indexes = new AtomicInteger[]{
            new AtomicInteger(-1), //starting
            new AtomicInteger(-1) //search
    };

    public BukkitSignManagement(BukkitCloudNetSignsPlugin plugin) {
        instance = this;
        AbstractSignManagement.instance = this;

        this.plugin = plugin;
        this.signs = Iterables.newCopyOnWriteArrayList();
        this.signs.addAll(getSignsFromNode());

        this.executeSearchingTask();
        this.executeStartingTask();
    }

    public static BukkitSignManagement getInstance() {
        return BukkitSignManagement.instance;
    }

    private void putService(ServiceInfoSnapshot serviceInfoSnapshot, Function<SignConfigurationEntry, ServiceInfoState> stateFunction) {
        if (!isImportantCloudService(serviceInfoSnapshot)) {
            return;
        }

        SignConfigurationEntry entry = getOwnSignConfigurationEntry();
        if (entry == null) {
            return;
        }

        this.services.put(serviceInfoSnapshot.getServiceId().getUniqueId(), new Pair<>(serviceInfoSnapshot, stateFunction.apply(entry)));
        this.updateSigns();
    }

    @Override
    public void onRegisterService(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> ServiceInfoState.STOPPED);
    }

    @Override
    public void onStartService(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> ServiceInfoState.STARTING);
    }

    @Override
    public void onConnectService(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> this.fromServiceInfoSnapshot(serviceInfoSnapshot, entry));
    }

    @Override
    public void onUpdateServiceInfo(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> this.fromServiceInfoSnapshot(serviceInfoSnapshot, entry));
    }

    @Override
    public void onDisconnectService(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> ServiceInfoState.STOPPED);
    }

    @Override
    public void onStopService(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.putService(serviceInfoSnapshot, entry -> ServiceInfoState.STOPPED);
    }

    @Override
    public void onUnregisterService(ServiceInfoSnapshot serviceInfoSnapshot) {
        if (!isImportantCloudService(serviceInfoSnapshot)) {
            return;
        }

        SignConfigurationEntry entry = getOwnSignConfigurationEntry();
        if (entry == null) {
            return;
        }

        services.remove(serviceInfoSnapshot.getServiceId().getUniqueId());
        this.updateSigns();
    }

    @Override
    public void onSignAdd(Sign sign) {
        Validate.checkNotNull(sign);

        this.signs.add(sign);
        CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
    }


    @Override
    public void onSignRemove(Sign sign) {
        Validate.checkNotNull(sign);

        Sign signEntry = Iterables.first(this.signs, s -> s.getSignId() == sign.getSignId());

        if (signEntry != null) {
            this.signs.remove(signEntry);
        }

        CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
    }

    public void updateSigns() {
        SignConfigurationEntry signConfiguration = getOwnSignConfigurationEntry();
        if (signConfiguration == null) {
            return;
        }

        List<Sign> signs = Iterables.newArrayList(Iterables.filter(this.signs, sign -> Iterables.contains(sign.getProvidedGroup(), Wrapper.getInstance().getServiceConfiguration().getGroups())));

        Collections.sort(signs);
        if (signs.isEmpty()) {
            return;
        }

        List<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>> cachedFilter = Iterables.newArrayList();
        List<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>> entries = Iterables.newArrayList(Iterables.filter(this.services.entrySet(),
                item -> item.getValue().getSecond() != ServiceInfoState.STOPPED));

        entries.sort(ENTRY_COMPARATOR);

        for (Sign sign : signs) {
            this.updateSign(sign, signConfiguration, cachedFilter, entries);
        }
    }

    private void updateSign(Sign sign,
                            SignConfigurationEntry signConfiguration,
                            List<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>> cachedFilter,
                            List<Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>>> entries) {
        Iterables.filter(entries, entry -> {
            boolean access = Iterables.contains(sign.getTargetGroup(), entry.getValue().getFirst().getConfiguration().getGroups());

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
        }, cachedFilter);

        cachedFilter.sort(ENTRY_COMPARATOR_2);

        if (!cachedFilter.isEmpty()) {
            Map.Entry<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> entry = cachedFilter.get(0);

            sign.setServiceInfoSnapshot(entry.getValue().getFirst());

            this.applyState(sign, signConfiguration, entry.getValue().getFirst(), entry.getValue().getSecond());

            entries.remove(entry);

        } else {
            sign.setServiceInfoSnapshot(null);

            if (!signConfiguration.getSearchLayouts().getSignLayouts().isEmpty()) {
                updateSignNext(sign, signConfiguration.getSearchLayouts().getSignLayouts().get(indexes[1].get()), null);
            }
        }

        cachedFilter.clear();
    }

    private void applyState(Sign sign, SignConfigurationEntry signConfiguration, ServiceInfoSnapshot serviceInfoSnapshot, ServiceInfoState state) {
        switch (state) {
            case STOPPED: {
                sign.setServiceInfoSnapshot(null);

                if (!signConfiguration.getSearchLayouts().getSignLayouts().isEmpty()) {
                    updateSignNext(sign, signConfiguration.getSearchLayouts().getSignLayouts().get(indexes[1].get()), null);
                }
            }
            break;
            case STARTING: {
                sign.setServiceInfoSnapshot(null);

                if (!signConfiguration.getStartingLayouts().getSignLayouts().isEmpty()) {
                    updateSignNext(sign, signConfiguration.getStartingLayouts().getSignLayouts().get(indexes[0].get()), serviceInfoSnapshot);
                }
            }
            break;
            case EMPTY_ONLINE: {
                SignLayout signLayout = null;

                SignConfigurationTaskEntry taskEntry = getValidSignConfigurationTaskEntryFromSignConfigurationEntry(signConfiguration, sign.getTargetGroup());

                if (taskEntry != null) {
                    signLayout = taskEntry.getEmptyLayout();
                }

                if (signLayout == null) {
                    signLayout = signConfiguration.getDefaultEmptyLayout();
                }

                updateSignNext(sign, signLayout, serviceInfoSnapshot);
            }
            break;
            case ONLINE: {
                SignLayout signLayout = null;

                SignConfigurationTaskEntry taskEntry = getValidSignConfigurationTaskEntryFromSignConfigurationEntry(signConfiguration, sign.getTargetGroup());

                if (taskEntry != null) {
                    signLayout = taskEntry.getOnlineLayout();
                }

                if (signLayout == null) {
                    signLayout = signConfiguration.getDefaultOnlineLayout();
                }

                updateSignNext(sign, signLayout, serviceInfoSnapshot);
            }
            break;
            case FULL_ONLINE: {
                SignLayout signLayout = null;

                SignConfigurationTaskEntry taskEntry = getValidSignConfigurationTaskEntryFromSignConfigurationEntry(signConfiguration, sign.getTargetGroup());

                if (taskEntry != null) {
                    signLayout = taskEntry.getFullLayout();
                }

                if (signLayout == null) {
                    signLayout = signConfiguration.getDefaultFullLayout();
                }

                updateSignNext(sign, signLayout, serviceInfoSnapshot);
            }
            break;
        }
    }

    private SignConfigurationTaskEntry getValidSignConfigurationTaskEntryFromSignConfigurationEntry(SignConfigurationEntry entry, String targetTask) {
        return Iterables.first(entry.getTaskLayouts(), signConfigurationTaskEntry -> signConfigurationTaskEntry.getTask() != null &&
                signConfigurationTaskEntry.getEmptyLayout() != null &&
                signConfigurationTaskEntry.getFullLayout() != null &&
                signConfigurationTaskEntry.getOnlineLayout() != null &&
                signConfigurationTaskEntry.getTask().equalsIgnoreCase(targetTask));
    }

    private void updateSignNext(Sign sign, SignLayout signLayout, ServiceInfoSnapshot serviceInfoSnapshot) {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            Location location = toLocation(sign.getWorldPosition());

            if (location == null) {
                super.sendSignRemoveUpdate(sign);
                return;
            }

            Block block = location.getBlock();

            if (!(block.getState() instanceof org.bukkit.block.Sign)) {
                super.sendSignRemoveUpdate(sign);
                return;
            }

            org.bukkit.block.Sign bukkitSign = (org.bukkit.block.Sign) block.getState();

            updateSign(location, sign, bukkitSign, signLayout, serviceInfoSnapshot);
        });
    }

    private void updateSign(Location location, Sign sign, org.bukkit.block.Sign bukkitSign, SignLayout signLayout, ServiceInfoSnapshot serviceInfoSnapshot) //serviceInfoSnapshot nullable
    {
        Validate.checkNotNull(location);
        Validate.checkNotNull(bukkitSign);
        Validate.checkNotNull(signLayout);

        if (signLayout.getLines() != null &&
                signLayout.getLines().length == 4) {
            String[] lines = new String[4];

            for (int i = 0; i < lines.length; i++) {
                lines[i] = addDataToLine(sign, signLayout.getLines()[i], serviceInfoSnapshot);
            }

            bukkitSign.setLine(0, lines[0]);
            bukkitSign.setLine(1, lines[1]);
            bukkitSign.setLine(2, lines[2]);
            bukkitSign.setLine(3, lines[3]);
            bukkitSign.update();

            this.changeBlock(location, signLayout.getBlockType(), signLayout.getSubId());
        }
    }

    private String addDataToLine(Sign sign, String input, ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(input);

        if (serviceInfoSnapshot == null) {
            return ChatColor.translateAlternateColorCodes('&', input);
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
        input = input.replace("%motd%", serviceInfoSnapshot.getProperties().getString("Motd"));
        input = input.replace("%extra%", serviceInfoSnapshot.getProperties().getString("Extra"));
        input = input.replace("%state%", serviceInfoSnapshot.getProperties().getString("State"));
        input = input.replace("%version%", serviceInfoSnapshot.getProperties().getString("Version"));
        input = input.replace("%whitelist%", (serviceInfoSnapshot.getProperties().contains("Whitelist-Enabled") &&
                serviceInfoSnapshot.getProperties().getBoolean("Whitelist-Enabled")
                ? "Enabled" : "Disabled"
        ));

        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private void changeBlock(Location location, String blockType, int subId) {
        Validate.checkNotNull(location);

        if (blockType != null && subId != -1) {

            BlockState signBlockState = location.getBlock().getState();
            MaterialData signMaterialData = signBlockState.getData();

            BlockFace signBlockFace;

            if (signMaterialData instanceof org.bukkit.material.Sign) { // will return false on 1.14+, even if it's a sign
                org.bukkit.material.Sign sign = (org.bukkit.material.Sign) signMaterialData;
                signBlockFace = sign.getFacing();
            } else { // trying to get the facing over directionals from the 1.13+ api
                signBlockFace = this.getDirectionalFacing(signBlockState);
            }

            if (signBlockFace != null) {

                BlockState backBlockState = location.getBlock().getRelative(signBlockFace.getOppositeFace()).getState();
                Material backBlockMaterial = Material.getMaterial(blockType.toUpperCase());

                if (backBlockMaterial != null) {
                    backBlockState.setType(backBlockMaterial);
                    backBlockState.setData(new MaterialData(backBlockMaterial, (byte) subId));
                    backBlockState.update(true);
                }

            }

        }
    }

    /**
     * Returns the facing of the specified block face, if its block data is an {@link org.bukkit.block.data.Directional}
     * from the 1.13+ spigot api
     *
     * @param blockState the block state the facing should be returned from
     * @return the facing of the block state
     */
    private BlockFace getDirectionalFacing(BlockState blockState) {
        try {

            Method getBlockDataMethod = BlockState.class.getDeclaredMethod("getBlockData");
            Object blockData = getBlockDataMethod.invoke(blockState);

            Class<?> directionalClass = Class.forName("org.bukkit.block.data.Directional");

            if (directionalClass.isInstance(blockData)) {
                Method getFacingMethod = directionalClass.getDeclaredMethod("getFacing");

                return (BlockFace) getFacingMethod.invoke(blockData);
            }

            return null;

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return null;
        }
    }

    public Location toLocation(SignPosition signPosition) {
        Validate.checkNotNull(signPosition);

        return Bukkit.getWorld(signPosition.getWorld()) != null ? new Location(
                Bukkit.getWorld(signPosition.getWorld()),
                signPosition.getX(),
                signPosition.getY(),
                signPosition.getZ()
        ) : null;
    }

    public SignConfigurationEntry getOwnSignConfigurationEntry() {
        return Iterables.first(SignConfigurationProvider.load().getConfigurations(), signConfigurationEntry -> Iterables.contains(signConfigurationEntry.getTargetGroup(), Wrapper.getInstance().getServiceConfiguration().getGroups()));
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
        if (isIngameService(serviceInfoSnapshot)) {
            return ServiceInfoState.STOPPED;
        }

        if (isEmptyService(serviceInfoSnapshot)) {
            return ServiceInfoState.EMPTY_ONLINE;
        }

        if (isFullService(serviceInfoSnapshot)) {
            if (!signConfiguration.isSwitchToSearchingWhenServiceIsFull()) {
                return ServiceInfoState.FULL_ONLINE;
            } else {
                return ServiceInfoState.STOPPED;
            }
        }

        if (isStartingService(serviceInfoSnapshot)) {
            return ServiceInfoState.STARTING;
        }

        if (serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING &&
                serviceInfoSnapshot.isConnected() &&
                serviceInfoSnapshot.getProperties().getBoolean("Online")) {
            return ServiceInfoState.ONLINE;
        } else {
            return ServiceInfoState.STOPPED;
        }
    }

    private void executeStartingTask() {
        SignConfigurationEntry signConfigurationEntry = getOwnSignConfigurationEntry();

        if (signConfigurationEntry != null && signConfigurationEntry.getStartingLayouts() != null &&
                signConfigurationEntry.getStartingLayouts().getSignLayouts().size() > 0) {
            if (indexes[0].get() == -1) {
                indexes[0].set(0);
            }

            if ((indexes[0].get() + 1) < signConfigurationEntry.getStartingLayouts().getSignLayouts().size()) {
                indexes[0].incrementAndGet();
            } else {
                indexes[0].set(0);
            }

            Bukkit.getScheduler().runTaskLater(
                    this.plugin,
                    this::executeStartingTask,
                    20 / (Math.min(signConfigurationEntry.getStartingLayouts().getAnimationsPerSecond(), 20))
            );
        } else {
            indexes[0].set(-1);
            Bukkit.getScheduler().runTaskLater(this.plugin, this::executeStartingTask, 20);
        }

        CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
    }

    private void executeSearchingTask() {
        SignConfigurationEntry signConfigurationEntry = getOwnSignConfigurationEntry();

        if (signConfigurationEntry != null && signConfigurationEntry.getSearchLayouts() != null &&
                signConfigurationEntry.getSearchLayouts().getSignLayouts().size() > 0) {
            if (indexes[1].get() == -1) {
                indexes[1].set(0);
            }

            if ((indexes[1].get() + 1) < signConfigurationEntry.getSearchLayouts().getSignLayouts().size()) {
                indexes[1].incrementAndGet();
            } else {
                indexes[1].set(0);
            }

            Bukkit.getScheduler().runTaskLater(
                    this.plugin,
                    this::executeSearchingTask,
                    20 / (Math.min(signConfigurationEntry.getSearchLayouts().getAnimationsPerSecond(), 20))
            );
        } else {
            indexes[1].set(-1);
            Bukkit.getScheduler().runTaskLater(this.plugin, this::executeSearchingTask, 20);
        }

        CloudNetDriver.getInstance().getTaskScheduler().schedule(this::updateSigns);
    }

    public Map<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> getServices() {
        return this.services;
    }

    public BukkitCloudNetSignsPlugin getPlugin() {
        return this.plugin;
    }

    public AtomicInteger[] getIndexes() {
        return this.indexes;
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