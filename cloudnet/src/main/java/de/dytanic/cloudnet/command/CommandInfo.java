package de.dytanic.cloudnet.command;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * The commandInfo class allows to easy serialize the command information
 */
@Getter
@AllArgsConstructor
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
}