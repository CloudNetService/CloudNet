package de.dytanic.cloudnet.provider;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class NodeGroupConfigurationProvider implements GroupConfigurationProvider {

    private static final Path GROUPS_FILE = Paths.get(System.getProperty("cloudnet.config.groups.path", "local/groups.json"));
    private static final Type TYPE = TypeToken.getParameterized(Collection.class, GroupConfiguration.class).getType();

    private final CloudNet cloudNet;

    private final Collection<GroupConfiguration> groups = new CopyOnWriteArrayList<>();

    public NodeGroupConfigurationProvider(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    public boolean isFileCreated() {
        return Files.exists(GROUPS_FILE);
    }

    private void loadGroups() {
        this.groups.clear();
        JsonDocument document = JsonDocument.newDocument(GROUPS_FILE);
        if (document.contains("groups")) {
            this.groups.addAll(document.get("groups", TYPE));
        }
    }

    public void writeGroups() {
        new JsonDocument().append("groups", this.groups).write(GROUPS_FILE);
    }

    @Override
    public void reload() {
        this.loadGroups();
    }

    @Override
    public Collection<GroupConfiguration> getGroupConfigurations() {
        return Collections.unmodifiableCollection(this.groups);
    }

    @Override
    public void setGroupConfigurations(@NotNull Collection<GroupConfiguration> groupConfigurations) {
        this.setGroupConfigurationsWithoutClusterSync(groupConfigurations);
        this.cloudNet.updateGroupConfigurationsInCluster(groupConfigurations, NetworkUpdateType.SET);
    }

    public void setGroupConfigurationsWithoutClusterSync(Collection<GroupConfiguration> groupConfigurations) {
        this.groups.clear();
        this.groups.addAll(groupConfigurations);
        this.writeGroups();
    }

    @Nullable
    @Override
    public GroupConfiguration getGroupConfiguration(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.getGroupConfigurations().stream().filter(groupConfiguration -> groupConfiguration.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    @Override
    public boolean isGroupConfigurationPresent(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.getGroupConfiguration(name) != null;
    }

    @Override
    public void addGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
        Preconditions.checkNotNull(groupConfiguration);

        this.addGroupConfigurationWithoutClusterSync(groupConfiguration);
        CloudNet.getInstance().updateGroupConfigurationsInCluster(Collections.singletonList(groupConfiguration), NetworkUpdateType.ADD);
    }

    public void addGroupConfigurationWithoutClusterSync(@NotNull GroupConfiguration groupConfiguration) {
        Preconditions.checkNotNull(groupConfiguration);

        for (GroupConfiguration group : this.groups) {
            if (group.getName().equalsIgnoreCase(groupConfiguration.getName())) {
                this.groups.remove(groupConfiguration);
            }
        }

        this.groups.add(groupConfiguration);
        this.writeGroups();
    }

    @Override
    public void removeGroupConfiguration(@NotNull String name) {
        Preconditions.checkNotNull(name);

        GroupConfiguration configuration = this.removeGroupConfigurationWithoutClusterSync(name);
        if (configuration != null) {
            this.cloudNet.updateGroupConfigurationsInCluster(Collections.singletonList(configuration), NetworkUpdateType.REMOVE);
        }
    }

    public GroupConfiguration removeGroupConfigurationWithoutClusterSync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        GroupConfiguration configuration = this.getGroupConfiguration(name);
        if (configuration != null) {
            this.groups.remove(configuration);
            this.writeGroups();
        }

        return configuration;
    }

    @Override
    public void removeGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
        Preconditions.checkNotNull(groupConfiguration);

        this.removeGroupConfiguration(groupConfiguration.getName());
    }

    @Override
    public @NotNull ITask<Void> reloadAsync() {
        return this.cloudNet.scheduleTask(() -> {
            this.reload();
            return null;
        });
    }

    @Override
    @NotNull
    public ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync() {
        return this.cloudNet.scheduleTask(this::getGroupConfigurations);
    }

    @Override
    public @NotNull ITask<Void> setGroupConfigurationsAsync(@NotNull Collection<GroupConfiguration> groupConfigurations) {
        return this.cloudNet.scheduleTask(() -> {
            this.setGroupConfigurations(groupConfigurations);
            return null;
        });
    }

    @Override
    @NotNull
    public ITask<GroupConfiguration> getGroupConfigurationAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> this.getGroupConfiguration(name));
    }

    @Override
    @NotNull
    public ITask<Boolean> isGroupConfigurationPresentAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> this.isGroupConfigurationPresent(name));
    }

    @Override
    @NotNull
    public ITask<Void> addGroupConfigurationAsync(@NotNull GroupConfiguration groupConfiguration) {
        return this.cloudNet.scheduleTask(() -> {
            this.addGroupConfiguration(groupConfiguration);
            return null;
        });
    }

    @Override
    @NotNull
    public ITask<Void> removeGroupConfigurationAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> {
            this.removeGroupConfiguration(name);
            return null;
        });
    }

    @Override
    @NotNull
    public ITask<Void> removeGroupConfigurationAsync(@NotNull GroupConfiguration groupConfiguration) {
        return this.cloudNet.scheduleTask(() -> {
            this.removeGroupConfiguration(groupConfiguration);
            return null;
        });
    }

}
