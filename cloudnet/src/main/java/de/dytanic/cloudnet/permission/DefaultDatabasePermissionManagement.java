package de.dytanic.cloudnet.permission;

import com.google.gson.reflect.TypeToken;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.permission.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class DefaultDatabasePermissionManagement implements ClusterSynchronizedPermissionManagement {

    private static final String DATABASE_USERS_NAME = "cloudnet_permission_users";

    private final File file = new File(System.getProperty("cloudnet.permissions.json.path", "local/permissions.json"));

    private final Map<String, IPermissionGroup> permissionGroupsMap = new ConcurrentHashMap<>();
    private final Callable<AbstractDatabaseProvider> databaseProviderCallable;
    private IPermissionManagementHandler permissionManagementHandler;

    public DefaultDatabasePermissionManagement(Callable<AbstractDatabaseProvider> databaseProviderCallable) {
        this.databaseProviderCallable = databaseProviderCallable;

        this.file.getParentFile().mkdirs();
        this.loadGroups();
    }

    @Override
    public IPermissionUser addUserWithoutClusterSync(IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        this.getDatabase().insert(permissionUser.getUniqueId().toString(), new JsonDocument(permissionUser));
        return permissionUser;
    }

    @Override
    public void updateUserWithoutClusterSync(IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        this.getDatabase().update(permissionUser.getUniqueId().toString(), new JsonDocument(permissionUser));
    }

    @Override
    public void deleteUserWithoutClusterSync(String name) {
        Preconditions.checkNotNull(name);

        for (IPermissionUser permissionUser : this.getUsers(name)) {
            this.getDatabase().delete(permissionUser.getUniqueId().toString());
        }
    }

    @Override
    public void deleteUserWithoutClusterSync(IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        this.getDatabase().delete(permissionUser.getUniqueId().toString());
    }

    @Override
    public boolean containsUser(UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.getDatabase().contains(uniqueId.toString());
    }

    @Override
    public boolean containsUser(String name) {
        Preconditions.checkNotNull(name);

        return this.getUsers(name).size() > 0;
    }

    @Override
    public IPermissionUser getUser(UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        JsonDocument jsonDocument = this.getDatabase().get(uniqueId.toString());

        if (jsonDocument != null) {
            IPermissionUser permissionUser = jsonDocument.toInstanceOf(PermissionUser.TYPE);

            if (this.testPermissionUser(permissionUser)) {
                this.updateUser(permissionUser);
            }

            return permissionUser;
        } else {
            return null;
        }
    }

    @Override
    public List<IPermissionUser> getUsers(String name) {
        Preconditions.checkNotNull(name);

        return this.getDatabase().get("name", name).stream().map(strings -> {
            IPermissionUser permissionUser = strings.toInstanceOf(PermissionUser.TYPE);

            if (this.testPermissionUser(permissionUser)) {
                this.updateUser(permissionUser);
            }

            return permissionUser;
        }).collect(Collectors.toList());
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        Collection<IPermissionUser> permissionUsers = new ArrayList<>();

        this.getDatabase().iterate((s, strings) -> {
            IPermissionUser permissionUser = strings.toInstanceOf(PermissionUser.TYPE);
            this.testPermissionUser(permissionUser);

            permissionUsers.add(permissionUser);
        });

        return permissionUsers;
    }

    @Override
    public void setUsersWithoutClusterSync(Collection<? extends IPermissionUser> users) {
        Preconditions.checkNotNull(users);

        this.getDatabase().clear();

        for (IPermissionUser permissionUser : users) {
            if (permissionUser != null) {
                this.getDatabase().insert(permissionUser.getUniqueId().toString(), new JsonDocument(permissionUser));
            }
        }
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(String group) {
        Preconditions.checkNotNull(group);

        Collection<IPermissionUser> permissionUsers = new ArrayList<>();

        this.getDatabase().iterate((s, strings) -> {
            IPermissionUser permissionUser = strings.toInstanceOf(PermissionUser.TYPE);

            this.testPermissionUser(permissionUser);
            if (permissionUser.inGroup(group)) {
                permissionUsers.add(permissionUser);
            }
        });

        return permissionUsers;
    }


    @Override
    public IPermissionGroup addGroupWithoutClusterSync(IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        this.testPermissionGroup(permissionGroup);
        if (this.getGroup(permissionGroup.getName()) != null) {
            this.deleteGroup(permissionGroup.getName());
        }
        this.permissionGroupsMap.put(permissionGroup.getName(), permissionGroup);
        this.saveGroups();

        return permissionGroup;
    }

    @Override
    public void updateGroupWithoutClusterSync(IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        this.testPermissionGroup(permissionGroup);
        this.permissionGroupsMap.put(permissionGroup.getName(), permissionGroup);

        this.saveGroups();
    }

    @Override
    public void deleteGroupWithoutClusterSync(String group) {
        Preconditions.checkNotNull(group);

        IPermissionGroup permissionGroup = this.permissionGroupsMap.remove(group);
        if (permissionGroup != null) {
            this.saveGroups();
        }
    }

    @Override
    public void deleteGroupWithoutClusterSync(IPermissionGroup group) {
        Preconditions.checkNotNull(group);

        this.deleteGroupWithoutClusterSync(group.getName());
    }

    @Override
    public boolean containsGroup(String name) {
        Preconditions.checkNotNull(name);

        return this.permissionGroupsMap.containsKey(name);
    }

    @Override
    public IPermissionGroup getGroup(String name) {
        Preconditions.checkNotNull(name);

        IPermissionGroup permissionGroup = this.permissionGroupsMap.get(name);

        if (this.testPermissionGroup(permissionGroup)) {
            this.updateGroup(permissionGroup);
        }

        return permissionGroup;
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        for (IPermissionGroup permissionGroup : this.permissionGroupsMap.values()) {
            if (this.testPermissionGroup(permissionGroup)) {
                this.updateGroup(permissionGroup);
            }
        }

        return this.permissionGroupsMap.values();
    }

    @Override
    public void setGroupsWithoutClusterSync(Collection<? extends IPermissionGroup> groups) {
        Preconditions.checkNotNull(groups);

        this.permissionGroupsMap.clear();

        for (IPermissionGroup group : groups) {
            this.testPermissionGroup(group);
            this.permissionGroupsMap.put(group.getName(), group);
        }

        this.saveGroups();
    }

    @Override
    public boolean reload() {
        loadGroups();

        if (this.permissionManagementHandler != null) {
            this.permissionManagementHandler.handleReloaded(this);
        }

        return true;
    }

    private void saveGroups() {
        List<IPermissionGroup> permissionGroups = new ArrayList<>(this.permissionGroupsMap.values());
        Collections.sort(permissionGroups);

        new JsonDocument("groups", permissionGroups).write(this.file);
    }

    private void loadGroups() {
        JsonDocument document = JsonDocument.newDocument(this.file);

        if (document.contains("groups")) {
            Collection<PermissionGroup> permissionGroups = document.get("groups", new TypeToken<Collection<PermissionGroup>>() {
            }.getType());

            this.permissionGroupsMap.clear();

            for (PermissionGroup group : permissionGroups) {
                this.permissionGroupsMap.put(group.getName(), group);
            }

            // saving the groups again to be sure that new fields in the permission group are in the file too
            document.append("groups", this.permissionGroupsMap.values());
            document.write(this.file);
        }
    }

    public IDatabase getDatabase() {
        return this.getDatabaseProvider().getDatabase(DATABASE_USERS_NAME);
    }

    private AbstractDatabaseProvider getDatabaseProvider() {
        try {
            return this.databaseProviderCallable.call();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    public Map<String, IPermissionGroup> getPermissionGroupsMap() {
        return this.permissionGroupsMap;
    }

    public Callable<AbstractDatabaseProvider> getDatabaseProviderCallable() {
        return this.databaseProviderCallable;
    }

    public IPermissionManagementHandler getPermissionManagementHandler() {
        return this.permissionManagementHandler;
    }

    public void setPermissionManagementHandler(IPermissionManagementHandler permissionManagementHandler) {
        this.permissionManagementHandler = permissionManagementHandler;
    }
}
