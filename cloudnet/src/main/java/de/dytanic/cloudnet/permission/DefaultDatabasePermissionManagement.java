package de.dytanic.cloudnet.permission;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.permission.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

public final class DefaultDatabasePermissionManagement implements ClusterSynchronizedPermissionManagement {

    private static final String DATABASE_USERS_NAME = "cloudnet_permission_users";

    private final File file = new File(System.getProperty("cloudnet.permissions.json.path", "local/permissions.json"));

    private final Map<String, IPermissionGroup> permissionGroupsMap = Maps.newConcurrentHashMap();
    private final Callable<AbstractDatabaseProvider> databaseProviderCallable;
    private IPermissionManagementHandler permissionManagementHandler;

    public DefaultDatabasePermissionManagement(Callable<AbstractDatabaseProvider> databaseProviderCallable) {
        this.databaseProviderCallable = databaseProviderCallable;

        file.getParentFile().mkdirs();
        loadGroups();
    }

    @Override
    public IPermissionUser addUserWithoutClusterSync(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        getDatabase().insert(permissionUser.getUniqueId().toString(), new JsonDocument(permissionUser));
        return permissionUser;
    }

    @Override
    public void updateUserWithoutClusterSync(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        getDatabase().update(permissionUser.getUniqueId().toString(), new JsonDocument(permissionUser));
    }

    @Override
    public void deleteUserWithoutClusterSync(String name) {
        Validate.checkNotNull(name);

        for (IPermissionUser permissionUser : getUser(name)) {
            getDatabase().delete(permissionUser.getUniqueId().toString());
        }
    }

    @Override
    public void deleteUserWithoutClusterSync(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        getDatabase().delete(permissionUser.getUniqueId().toString());
    }

    @Override
    public boolean containsUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return getDatabase().contains(uniqueId.toString());
    }

    @Override
    public boolean containsUser(String name) {
        Validate.checkNotNull(name);

        return getUser(name).size() > 0;
    }

    @Override
    public IPermissionUser getUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        JsonDocument jsonDocument = getDatabase().get(uniqueId.toString());

        if (jsonDocument != null) {
            IPermissionUser permissionUser = jsonDocument.toInstanceOf(PermissionUser.TYPE);

            if (testPermissionUser(permissionUser)) {
                updateUser(permissionUser);
            }

            return permissionUser;
        } else {
            return null;
        }
    }

    @Override
    public List<IPermissionUser> getUsers(String name) {
        Validate.checkNotNull(name);

        return Iterables.map(getDatabase().get("name", name), strings -> {
            IPermissionUser permissionUser = strings.toInstanceOf(PermissionUser.TYPE);

            if (testPermissionUser(permissionUser)) {
                updateUser(permissionUser);
            }

            return permissionUser;
        });
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        Collection<IPermissionUser> permissionUsers = Iterables.newArrayList();

        getDatabase().iterate((s, strings) -> {
            IPermissionUser permissionUser = strings.toInstanceOf(PermissionUser.TYPE);
            testPermissionUser(permissionUser);

            permissionUsers.add(permissionUser);
        });

        return permissionUsers;
    }

    @Override
    public void setUsersWithoutClusterSync(Collection<? extends IPermissionUser> users) {
        Validate.checkNotNull(users);

        getDatabase().clear();

        for (IPermissionUser permissionUser : users) {
            if (permissionUser != null) {
                getDatabase().insert(permissionUser.getUniqueId().toString(), new JsonDocument(permissionUser));
            }
        }
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(String group) {
        Validate.checkNotNull(group);

        Collection<IPermissionUser> permissionUsers = Iterables.newArrayList();

        getDatabase().iterate((s, strings) -> {
            IPermissionUser permissionUser = strings.toInstanceOf(PermissionUser.TYPE);

            testPermissionUser(permissionUser);
            if (permissionUser.inGroup(group)) {
                permissionUsers.add(permissionUser);
            }
        });

        return permissionUsers;
    }


    @Override
    public IPermissionGroup addGroupWithoutClusterSync(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        testPermissionGroup(permissionGroup);
        if (this.getGroup(permissionGroup.getName()) != null) {
            deleteGroup(permissionGroup.getName());
        }
        permissionGroupsMap.put(permissionGroup.getName(), permissionGroup);
        saveGroups();

        return permissionGroup;
    }

    @Override
    public void updateGroupWithoutClusterSync(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        testPermissionGroup(permissionGroup);
        permissionGroupsMap.put(permissionGroup.getName(), permissionGroup);

        saveGroups();
    }

    @Override
    public void deleteGroupWithoutClusterSync(String group) {
        Validate.checkNotNull(group);

        IPermissionGroup permissionGroup = permissionGroupsMap.remove(group);
        if (permissionGroup != null) {
            saveGroups();
        }
    }

    @Override
    public void deleteGroupWithoutClusterSync(IPermissionGroup group) {
        Validate.checkNotNull(group);

        deleteGroupWithoutClusterSync(group.getName());
    }

    @Override
    public boolean containsGroup(String name) {
        Validate.checkNotNull(name);

        return this.permissionGroupsMap.containsKey(name);
    }

    @Override
    public IPermissionGroup getGroup(String name) {
        Validate.checkNotNull(name);

        IPermissionGroup permissionGroup = permissionGroupsMap.get(name);

        if (testPermissionGroup(permissionGroup)) {
            updateGroup(permissionGroup);
        }

        return permissionGroup;
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        for (IPermissionGroup permissionGroup : permissionGroupsMap.values()) {
            if (testPermissionGroup(permissionGroup)) {
                updateGroup(permissionGroup);
            }
        }

        return permissionGroupsMap.values();
    }

    @Override
    public void setGroupsWithoutClusterSync(Collection<? extends IPermissionGroup> groups) {
        Validate.checkNotNull(groups);

        permissionGroupsMap.clear();

        for (IPermissionGroup group : groups) {
            testPermissionGroup(group);
            permissionGroupsMap.put(group.getName(), group);
        }

        saveGroups();
    }

    @Override
    public boolean reload() {
        loadGroups();

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleReloaded(this);
        }

        return true;
    }

    private void saveGroups() {
        List<IPermissionGroup> permissionGroups = Iterables.newArrayList(permissionGroupsMap.values());
        Collections.sort(permissionGroups);

        new JsonDocument("groups", permissionGroups).write(file);
    }

    private void loadGroups() {
        JsonDocument document = JsonDocument.newDocument(file);

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
        return getDatabaseProvider().getDatabase(DATABASE_USERS_NAME);
    }

    private AbstractDatabaseProvider getDatabaseProvider() {
        try {
            return databaseProviderCallable.call();
        } catch (Exception e) {
            e.printStackTrace();
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
