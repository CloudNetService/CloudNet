package de.dytanic.cloudnet.command;

/**
 * Represents a command that should be execute
 */
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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Command)) return false;
        final Command other = (Command) o;
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
        final Object this$prefix = this.getPrefix();
        final Object other$prefix = other.getPrefix();
        if (this$prefix == null ? other$prefix != null : !this$prefix.equals(other$prefix)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Command;
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
        final Object $prefix = this.getPrefix();
        result = result * PRIME + ($prefix == null ? 43 : $prefix.hashCode());
        return result;
    }
}