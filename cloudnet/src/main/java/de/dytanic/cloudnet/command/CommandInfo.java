package de.dytanic.cloudnet.command;

/**
 * The commandInfo class allows to easy serialize the command information
 */
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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CommandInfo)) return false;
        final CommandInfo other = (CommandInfo) o;
        if (!other.canEqual((Object) this)) return false;
        if (!java.util.Arrays.deepEquals(this.getNames(), other.getNames())) return false;
        final Object this$permission = this.getPermission();
        final Object other$permission = other.getPermission();
        if (this$permission == null ? other$permission != null : !this$permission.equals(other$permission))
            return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description))
            return false;
        final Object this$usage = this.getUsage();
        final Object other$usage = other.getUsage();
        if (this$usage == null ? other$usage != null : !this$usage.equals(other$usage)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CommandInfo;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getNames());
        final Object $permission = this.getPermission();
        result = result * PRIME + ($permission == null ? 43 : $permission.hashCode());
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $usage = this.getUsage();
        result = result * PRIME + ($usage == null ? 43 : $usage.hashCode());
        return result;
    }
}