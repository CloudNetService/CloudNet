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
@EqualsAndHashCode(callSuper = false)
public class PermissionGroup extends AbstractPermissible implements IPermissionGroup {

    /**
     * The Gson TypeToken result of the PermissionGroup class
     */
    public static final Type TYPE = new TypeToken<PermissionGroup>() {
    }.getType();

    protected Collection<String> groups = Iterables.newArrayList();

    private String prefix = "&7";
    private String color = "&7";
    private String suffix = "&f";
    private String display = "&7";

    private int sortId = 0;

    private boolean defaultGroup = false;

    public PermissionGroup() {
    }

    public PermissionGroup(String name, int potency) {
        super();

        this.name = name;
        this.potency = potency;
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