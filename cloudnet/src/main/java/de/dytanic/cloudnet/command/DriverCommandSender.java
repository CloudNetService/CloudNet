package de.dytanic.cloudnet.command;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.Collection;

/**
 * The driverCommandSender
 */
public final class DriverCommandSender implements ICommandSender {

  private final Collection<String> messages;

  public DriverCommandSender(Collection<String> messages) {
    this.messages = messages;
  }

  @Override
  public String getName() {
    return "DriverCommandSender";
  }

  @Override
  public void sendMessage(String message) {
    Preconditions.checkNotNull(message);

    this.messages.add(message);
  }

  @Override
  public void sendMessage(String... messages) {
    Preconditions.checkNotNull(messages);

    this.messages.addAll(Arrays.asList(messages));
  }

  @Override
  public boolean hasPermission(String permission) {
    return true;
  }
}
