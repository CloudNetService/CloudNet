package de.dytanic.cloudnet.permission.command;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;

import java.util.Queue;

public interface IPermissionUserCommandSender extends ICommandSender {

    Queue<String> getWrittenMessages();

    IPermissionManagement getPermissionManagement();

    IPermissionUser getPermissionUser();

}