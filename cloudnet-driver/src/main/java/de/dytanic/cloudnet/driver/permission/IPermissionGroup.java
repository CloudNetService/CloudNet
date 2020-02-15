package de.dytanic.cloudnet.driver.permission;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface IPermissionGroup extends IPermissible {

    Collection<String> getGroups();

    boolean isDefaultGroup();

    void setDefaultGroup(boolean defaultGroup);

    int getSortId();

    void setSortId(int sortId);

    String getPrefix();

    void setPrefix(@NotNull String prefix);

    String getColor();

    void setColor(@NotNull String color);

    String getSuffix();

    void setSuffix(@NotNull String suffix);

    String getDisplay();

    void setDisplay(@NotNull String display);

}