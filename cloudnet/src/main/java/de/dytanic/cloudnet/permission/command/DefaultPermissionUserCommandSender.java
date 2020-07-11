package de.dytanic.cloudnet.permission.command;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class DefaultPermissionUserCommandSender implements IPermissionUserCommandSender {

    protected final IPermissionUser permissionUser;

    protected final IPermissionManagement permissionManagement;

    protected final Queue<String> writtenMessages = new ConcurrentLinkedQueue<>();

    public DefaultPermissionUserCommandSender(IPermissionUser permissionUser, IPermissionManagement permissionManagement) {
        this.permissionUser = permissionUser;
        this.permissionManagement = permissionManagement;
    }

    @Override
    public String getName() {
        return this.permissionUser.getName();
    }

    @Override
    public void sendMessage(String message) {
        Preconditions.checkNotNull(message);

        this.writtenMessages.add(message);

        while (this.writtenMessages.size() > 64) {
            this.writtenMessages.poll();
        }
    }

    @Override
    public void sendMessage(String... messages) {
        Preconditions.checkNotNull(messages);

        for (String message : messages) {
            this.sendMessage(message);
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.permissionManagement.hasPermission(this.permissionUser, permission);
    }

    public IPermissionUser getPermissionUser() {
        return this.permissionUser;
    }

    public IPermissionManagement getPermissionManagement() {
        return this.permissionManagement;
    }

    public Queue<String> getWrittenMessages() {
        return this.writtenMessages;
    }
}