package de.dytanic.cloudnet.command;

import de.dytanic.cloudnet.common.command.CommandInfo;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents a command that should be execute
 */
@ToString
@EqualsAndHashCode
public abstract class Command implements ICommandExecutor {

    protected String[] names;

    protected String permission;

    protected String description;

    protected String usage;

    protected String prefix;

    public Command(String... names) {
        this.names = names;
    }

    public Command(String[] names, String permission) {
        this.names = names;
        this.permission = permission;
    }

    public Command(String[] names, String permission, String description) {
        this.names = names;
        this.permission = permission;
        this.description = description;
    }

    public Command(String[] names, String permission, String description, String usage, String prefix) {
        this.names = names;
        this.permission = permission;
        this.description = description;
        this.usage = usage;
        this.prefix = prefix;
    }

    public Command() {
    }

    public CommandInfo getInfo() {
        return new CommandInfo(this.names, permission, description, usage);
    }

    public final boolean isValid() {
        return this.names != null && this.names.length > 0 && this.names[0] != null && !this.names[0].isEmpty();
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

    public String getPrefix() {
        return this.prefix;
    }

}