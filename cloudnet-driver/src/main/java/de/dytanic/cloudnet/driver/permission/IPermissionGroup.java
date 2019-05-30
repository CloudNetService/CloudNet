package de.dytanic.cloudnet.driver.permission;

import java.util.Collection;

public interface IPermissionGroup extends IPermissible {

  Collection<String> getGroups();

  boolean isDefaultGroup();

  int getSortId();

  String getPrefix();

  String getSuffix();

  String getDisplay();

  void setDefaultGroup(boolean defaultGroup);

  void setSortId(int sortId);

  void setPrefix(String prefix);

  void setSuffix(String suffix);

  void setDisplay(String display);

}