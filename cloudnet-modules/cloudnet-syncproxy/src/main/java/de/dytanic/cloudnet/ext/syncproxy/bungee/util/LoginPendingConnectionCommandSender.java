package de.dytanic.cloudnet.ext.syncproxy.bungee.util;

import de.dytanic.cloudnet.common.Validate;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PermissionCheckEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class LoginPendingConnectionCommandSender implements CommandSender {

    private final Collection<String> permissions = new ArrayList<>(), groups = new ArrayList<>();

    private final PendingConnection pendingConnection;

    private final UUID uniqueId;

    public LoginPendingConnectionCommandSender(PendingConnection pendingConnection) {
        this.pendingConnection = pendingConnection;
        this.uniqueId = pendingConnection.getUniqueId();

        this.groups.addAll(ProxyServer.getInstance().getConfigurationAdapter().getGroups(pendingConnection.getName()));

        for (String group : groups) {
            for (String permission : ProxyServer.getInstance().getConfigurationAdapter().getPermissions(group)) {
                this.setPermission(permission, true);
            }
        }
    }

    @Override
    public String getName() {
        return this.pendingConnection.getName();
    }

    @Override
    public void sendMessage(String message) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void sendMessages(String... messages) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void sendMessage(BaseComponent... message) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void sendMessage(BaseComponent message) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void addGroups(String... groups) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void removeGroups(String... groups) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean hasPermission(String permission) {
        Validate.checkNotNull(permission);

        return ProxyServer.getInstance().getPluginManager().callEvent(new PermissionCheckEvent(
                this,
                permission,
                this.permissions.contains(permission.toLowerCase()))
        ).hasPermission();
    }

    @Override
    public void setPermission(String permission, boolean value) {
        Validate.checkNotNull(permission);

        if (value) {
            this.permissions.add(permission.toLowerCase());
        } else {
            this.permissions.remove(permission.toLowerCase());
        }
    }

    public Collection<String> getPermissions() {
        return this.permissions;
    }

    public Collection<String> getGroups() {
        return this.groups;
    }

    public PendingConnection getPendingConnection() {
        return this.pendingConnection;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }
}