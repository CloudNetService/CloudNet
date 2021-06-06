package de.dytanic.cloudnet.driver.command;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * The commandInfo class allows to easy serialize the command information
 */
@ToString
@EqualsAndHashCode
public class CommandInfo implements SerializableObject {

  /**
   * The configured names by the command
   */
  protected String[] names;

  /**
   * The permission, that is configured by this command, that the command sender should has
   */
  protected String permission;

  /**
   * The command description with a basic description
   */
  protected String description;

  /**
   * The easiest and important usage for the command
   */
  protected String usage;

  public CommandInfo(String[] names, String permission, String description, String usage) {
    this.names = names;
    this.permission = permission;
    this.description = description;
    this.usage = usage;
  }

  public CommandInfo() {
  }

  public String[] getNames() {
    return this.names;
  }

  public String getPermission() {
    return this.permission;
  }

  public String getDescription() {
    return this.description;
  }

  public String getUsage() {
    return this.usage;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeStringArray(this.names);
    buffer.writeOptionalString(this.permission);
    buffer.writeOptionalString(this.description);
    buffer.writeOptionalString(this.usage);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.names = buffer.readStringArray();
    this.permission = buffer.readOptionalString();
    this.description = buffer.readOptionalString();
    this.usage = buffer.readOptionalString();
  }
}
