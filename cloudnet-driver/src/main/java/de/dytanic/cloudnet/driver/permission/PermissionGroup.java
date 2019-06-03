package de.dytanic.cloudnet.driver.permission;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Iterables;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The default implementation of the IPermissionGroup class. This class should
 * use if you want to add new PermissionGroups into the IPermissionManagement
 * implementation
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionGroup extends AbstractPermissible implements
    IPermissionGroup {

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

  public PermissionGroup(String name, int potency, Collection<String> groups,
      String prefix, String suffix, String display, int sortId,
      boolean defaultGroup) {
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
}