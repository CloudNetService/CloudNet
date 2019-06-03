package de.dytanic.cloudnet.command;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a command that should be execute
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class Command implements ICommandExecutor {

    protected String[] names;

    protected String permission;

    protected String description;

    protected String usage;

    protected String prefix;

    public Command(String... names)
    {
        this.names = names;
    }

    public Command(String[] names, String permission)
    {
        this.names = names;
        this.permission = permission;
    }

    public Command(String[] names, String permission, String description)
    {
        this.names = names;
        this.permission = permission;
        this.description = description;
    }

    public Command(String[] names, String permission, String description, String usage, String prefix)
    {
        this.names = names;
        this.permission = permission;
        this.description = description;
        this.usage = usage;
        this.prefix = prefix;
    }

    public CommandInfo getInfo()
    {
        return new CommandInfo(this.names, permission, description, usage);
    }

    public final boolean isValid()
    {
        return this.names != null && this.names.length > 0 && this.names[0] != null && !this.names[0].isEmpty();
    }

}