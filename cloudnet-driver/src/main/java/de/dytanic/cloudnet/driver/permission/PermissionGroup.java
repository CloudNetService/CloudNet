package de.dytanic.cloudnet.driver.permission;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Iterables;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;
import java.util.Collection;

/**
 * The default implementation of the IPermissionGroup class. This class should use if you want to
 * add new PermissionGroups into the IPermissionManagement implementation
 */
@ToString
@EqualsAndHashCode
public class PermissionGroup extends AbstractPermissible implements IPermissionGroup {

    /**
     * The Gson TypeToken result of the PermissionGroup class
     */
    public static final Type TYPE = new TypeToken<PermissionGroup>() {
    }.getType();

    protected Collection<String> groups;

    private String prefix, color, suffix, display;

    private int sortId;

    private boolean defaultGroup;

    public PermissionGroup(String name, int potency) {
        super();

        this.name = name;
        this.potency = potency;
        this.groups = Iterables.newArrayList();
        this.prefix = "&7";
        this.color = "&7";
        this.suffix = "&f";
        this.display = "&7";
        this.sortId = 0;
        this.defaultGroup = false;
    }

    public PermissionGroup(String name, int potency, Collection<String> groups, String prefix, String color, String suffix, String display, int sortId, boolean defaultGroup) {
        super();

        this.name = name;
        this.potency = potency;
        this.groups = groups;
        this.prefix = prefix;
        this.color = color;
        this.suffix = suffix;
        this.display = display;
        this.sortId = sortId;
        this.defaultGroup = defaultGroup;
    }

    public Collection<String> getGroups() {
        return this.groups;
    }

    public void setGroups(Collection<String> groups) {
        this.groups = groups;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getDisplay() {
        return this.display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public int getSortId() {
        return this.sortId;
    }

    public void setSortId(int sortId) {
        this.sortId = sortId;
    }

    public boolean isDefaultGroup() {
        return this.defaultGroup;
    }

    public void setDefaultGroup(boolean defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

}