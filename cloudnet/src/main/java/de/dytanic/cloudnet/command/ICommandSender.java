package de.dytanic.cloudnet.command;

/**
 * The commandSender represents the sender for a command dispatch operation
 */
public interface ICommandSender {

  /**
   * Returns the name of the command sender
   */
  String getName();

  /**
   * Sends a message to the specific sender implementation
   *
   * @param message that should send
   */
  void sendMessage(String message);

  /**
   * Send the messages to the specific sender implementation
   *
   * @param messages that should send
   */
  void sendMessage(String... messages);

  /**
   * Checks, that the commandSender has the permission to execute the command
   *
   * @param permission the permission, that should that
   * @return true if the command sender is authorized that it has the permission
   */
  boolean hasPermission(String permission);
}
