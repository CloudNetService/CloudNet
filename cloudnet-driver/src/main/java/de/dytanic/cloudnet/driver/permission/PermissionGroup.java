package de.dytanic.cloudnet.driver.permission;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Iterables;

import java.lang.reflect.Type;
import java.util.Collection;

/**
 * The default implementation of the IPermissionGroup class. This class should use if you want to
 * add new PermissionGroups into the IPermissionManagement implementation
 */
public class PermissionGroup extends AbstractPermissible implements IPermissionGroup {

    /**
     * The Gson TypeToken result of the PermissionGroup class
     */
    public static final Type TYPE = new TypeToken<PermissionGroup>() {
    }.getType();

    protected Collection<String> groups;

    private String prefix, suffix, display;

    private int sortId;

    private boolean defaultGroup;

    public PermissionGroup(String name, int potency) {
        super();

        this.name = name;
        this.potency = potency;
        this.groups = Iterables.newArrayList();
        this.prefix = "&7";
        this.suffix = "&f";
        this.display = "&7";
        this.sortId = 0;
        this.defaultGroup = false;
    }

    public PermissionGroup(String name, int potency, Collection<String> groups, String prefix, String suffix, String display, int sortId, boolean defaultGroup) {
        super();

        this.name = name;
        this.potency = potency;
        this.groups = groups;
        this.prefix = prefix;
        this.suffix = suffix;
        this.display = display;
        this.sortId = sortId;
        this.defaultGroup = defaultGroup;
    }

    public Collection<String> getGroups() {
        return this.groups;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public String getDisplay() {
        return this.display;
    }

    public int getSortId() {
        return this.sortId;
    }

    public boolean isDefaultGroup() {
        return this.defaultGroup;
    }

    public void setGroups(Collection<String> groups) {
        this.groups = groups;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public void setSortId(int sortId) {
        this.sortId = sortId;
    }

    public void setDefaultGroup(boolean defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public String toString() {
        return "PermissionGroup(groups=" + this.getGroups() + ", prefix=" + this.getPrefix() + ", suffix=" + this.getSuffix() + ", display=" + this.getDisplay() + ", sortId=" + this.getSortId() + ", defaultGroup=" + this.isDefaultGroup() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PermissionGroup)) return false;
        final PermissionGroup other = (PermissionGroup) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$groups = this.getGroups();
        final Object other$groups = other.getGroups();
        if (this$groups == null ? other$groups != null : !this$groups.equals(other$groups)) return false;
        final Object this$prefix = this.getPrefix();
        final Object other$prefix = other.getPrefix();
        if (this$prefix == null ? other$prefix != null : !this$prefix.equals(other$prefix)) return false;
        final Object this$suffix = this.getSuffix();
        final Object other$suffix = other.getSuffix();
        if (this$suffix == null ? other$suffix != null : !this$suffix.equals(other$suffix)) return false;
        final Object this$display = this.getDisplay();
        final Object other$display = other.getDisplay();
        if (this$display == null ? other$display != null : !this$display.equals(other$display)) return false;
        if (this.getSortId() != other.getSortId()) return false;
        if (this.isDefaultGroup() != other.isDefaultGroup()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof PermissionGroup;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $groups = this.getGroups();
        result = result * PRIME + ($groups == null ? 43 : $groups.hashCode());
        final Object $prefix = this.getPrefix();
        result = result * PRIME + ($prefix == null ? 43 : $prefix.hashCode());
        final Object $suffix = this.getSuffix();
        result = result * PRIME + ($suffix == null ? 43 : $suffix.hashCode());
        final Object $display = this.getDisplay();
        result = result * PRIME + ($display == null ? 43 : $display.hashCode());
        result = result * PRIME + this.getSortId();
        result = result * PRIME + (this.isDefaultGroup() ? 79 : 97);
        return result;
    }
}