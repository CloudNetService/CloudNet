package de.dytanic.cloudnet.common.command;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * The commandInfo class allows to easy serialize the command information
 */
@ToString
@EqualsAndHashCode
public class CommandInfo {

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

}