package de.dytanic.cloudnet.driver.permission;

import java.util.Collection;

public interface IPermissionGroup extends IPermissible {

    Collection<String> getGroups();

    boolean isDefaultGroup();

    void setDefaultGroup(boolean defaultGroup);

    int getSortId();

    void setSortId(int sortId);

    String getPrefix();

    void setPrefix(String prefix);

    String getSuffix();

    void setSuffix(String suffix);

    String getDisplay();

    void setDisplay(String display);

}