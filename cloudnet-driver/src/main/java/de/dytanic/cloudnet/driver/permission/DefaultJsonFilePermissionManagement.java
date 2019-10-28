package de.dytanic.cloudnet.driver.permission;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultJsonFilePermissionManagement implements ClusterSynchronizedPermissionManagement {

    private final File file;

    private final Map<UUID, IPermissionUser> permissionUsers = Maps.newConcurrentHashMap();

    private final Map<String, IPermissionGroup> permissionGroups = Maps.newConcurrentHashMap();

    private IPermissionManagementHandler permissionManagementHandler;

    public DefaultJsonFilePermissionManagement(File file) {
        Validate.checkNotNull(file);

        this.file = file;
        this.file.getParentFile().mkdirs();

        this.load();
    }

    @Override
    public IPermissionUser addUserWithoutClusterSync(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        this.deleteUserWithoutClusterSync(permissionUser);
        this.permissionUsers.put(permissionUser.getUniqueId(), permissionUser);
        this.save();

        return permissionUser;
    }

    @Override
    public void updateUserWithoutClusterSync(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleUpdateUser(this, permissionUser);
        }

        testPermissionUser(permissionUser);
        this.permissionUsers.put(permissionUser.getUniqueId(), permissionUser);
        this.save();
    }

    @Override
    public void deleteUserWithoutClusterSync(String name) {
        Validate.checkNotNull(name);

        for (IPermissionUser permissionUser : Iterables.filter(this.permissionUsers.values(), permissionUser -> permissionUser.getName().equals(name))) {
            this.permissionUsers.remove(permissionUser.getUniqueId());
        }

        this.save();
    }

    @Override
    public void deleteUserWithoutClusterSync(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        this.permissionUsers.remove(permissionUser.getUniqueId());
        this.save();
    }

    @Override
    public boolean containsUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.permissionUsers.containsKey(uniqueId);
    }

    @Override
    public boolean containsUser(String name) {
        Validate.checkNotNull(name);

        return Iterables.first(permissionUsers.values(), permissionUser -> permissionUser.getName().equalsIgnoreCase(name)) != null;
    }

    @Override
    public IPermissionUser getUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);
        IPermissionUser permissionUser = this.permissionUsers.get(uniqueId);

        if (testPermissionUser(permissionUser)) {
            updateUser(permissionUser);
        }

        return permissionUser;
    }

    @Override
    public List<IPermissionUser> getUser(String name) {
        Validate.checkNotNull(name);

        List<IPermissionUser> permissionUsers = Iterables.filter(this.permissionUsers.values(), permissionUser -> permissionUser.getName().equals(name));

        for (IPermissionUser user : permissionUsers) {
            if (testPermissionUser(user)) {
                updateUser(user);
            }
        }

        return permissionUsers;
    }

    @Override
    public boolean reload() {
        load();

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleReloaded(this);
        }

        return true;
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        Collection<IPermissionUser> permissionUsers = this.permissionUsers.values();

        for (IPermissionUser permissionUser : permissionUsers) {
            if (testPermissionUser(permissionUser)) {
                updateUser(permissionUser);
            }
        }

        return permissionUsers;
    }

    @Override
    public void setUsersWithoutClusterSync(Collection<? extends IPermissionUser> users) {
        Validate.checkNotNull(users);

        this.permissionGroups.clear();

        for (IPermissionUser permissionUser : users) {
            testPermissionUser(permissionUser);
            this.permissionUsers.put(permissionUser.getUniqueId(), permissionUser);
        }

        this.save();
    }

    @Override
    public Collection<IPermissionUser> getUserByGroup(String group) {
        Validate.checkNotNull(group);

        return Iterables.filter(this.permissionUsers.values(), permissionUser -> permissionUser.inGroup(group));
    }

    @Override
    public IPermissionGroup addGroupWithoutClusterSync(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleAddGroup(this, permissionGroup);
        }

        this.permissionGroups.put(permissionGroup.getName(), permissionGroup);
        this.save();

        return permissionGroup;
    }

    @Override
    public void updateGroupWithoutClusterSync(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleUpdateGroup(this, permissionGroup);
        }

        testPermissionGroup(permissionGroup);
        this.permissionGroups.put(permissionGroup.getName(), permissionGroup);
        this.save();
    }

    @Override
    public void deleteGroupWithoutClusterSync(String group) {
        Validate.checkNotNull(group);
        IPermissionGroup permissionGroup = this.permissionGroups.remove(group);
        this.save();

        if (permissionGroup != null) {
            if (permissionManagementHandler != null) {
                permissionManagementHandler.handleDeleteGroup(this, permissionGroup);
            }
        }
    }

    @Override
    public void deleteGroupWithoutClusterSync(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        if (permissionManagementHandler != null) {
            permissionManagementHandler.handleDeleteGroup(this, permissionGroup);
        }

        this.permissionGroups.remove(permissionGroup.getName());
        this.save();
    }

    @Override
    public boolean containsGroup(String name) {
        Validate.checkNotNull(name);

        return permissionGroups.containsKey(name);
    }

    @Override
    public IPermissionGroup getGroup(String name) {
        Validate.checkNotNull(name);
        IPermissionGroup permissionGroup = this.permissionGroups.get(name);

        if (permissionGroup != null && testPermissionGroup(permissionGroup)) {
            updateGroup(permissionGroup);
        }

        return permissionGroup;
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        Collection<IPermissionGroup> permissionGroups = this.permissionGroups.values();

        for (IPermissionGroup permissionGroup : permissionGroups) {
            if (testPermissionGroup(permissionGroup)) {
                updateGroup(permissionGroup);
            }
        }

        return permissionGroups;
    }

    @Override
    public void setGroupsWithoutClusterSync(Collection<? extends IPermissionGroup> permissionGroups) {
        if (permissionGroups == null) {
            return;
        }

        this.permissionGroups.clear();

        for (IPermissionGroup permissionGroup : permissionGroups) {
            testPermissionGroup(permissionGroup);
            this.permissionGroups.put(permissionGroup.getName(), permissionGroup);
        }

        this.save();
    }

    private void save() {
        new JsonDocument()
                .append("groups", this.permissionGroups.values())
                .append("users", this.permissionUsers.values())
                .write(this.file);
    }

    private void load() {
        JsonDocument document = JsonDocument.newDocument(this.file);

        setUsers(document.get("users", new TypeToken<Collection<PermissionUser>>() {
        }.getType()));

        setGroups(document.get("groups", new TypeToken<Collection<PermissionGroup>>() {
        }.getType()));
    }

    public IPermissionManagementHandler getPermissionManagementHandler() {
        return this.permissionManagementHandler;
    }

    public void setPermissionManagementHandler(IPermissionManagementHandler permissionManagementHandler) {
        this.permissionManagementHandler = permissionManagementHandler;
    }
}