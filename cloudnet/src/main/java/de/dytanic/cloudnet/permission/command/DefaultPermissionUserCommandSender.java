package de.dytanic.cloudnet.permission.command;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;

import java.util.Queue;

public final class DefaultPermissionUserCommandSender implements IPermissionUserCommandSender {

    protected final IPermissionUser permissionUser;

    protected final IPermissionManagement permissionManagement;

    protected final Queue<String> writtenMessages = Iterables.newConcurrentLinkedQueue();

    public DefaultPermissionUserCommandSender(IPermissionUser permissionUser, IPermissionManagement permissionManagement) {
        this.permissionUser = permissionUser;
        this.permissionManagement = permissionManagement;
    }

    @Override
    public String getName() {
        return permissionUser.getName();
    }

    @Override
    public void sendMessage(String message) {
        Validate.checkNotNull(message);

        this.writtenMessages.add(message);

        while (this.writtenMessages.size() > 64) this.writtenMessages.poll();
    }

    @Override
    public void sendMessage(String... messages) {
        Validate.checkNotNull(messages);

        for (String message : messages)
            this.sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissionManagement.hasPermission(this.permissionUser, permission);
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