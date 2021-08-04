/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.driver.permission;

import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * This interfaces provides access to the properties of a permission group
 */
public interface IPermissionGroup extends IPermissible {

  /**
   * @return a collection of string with the group names that this group inherits from
   */
  Collection<String> getGroups();

  /**
   * @return whether the group is the default group and is assigned to new players or not
   */
  boolean isDefaultGroup();

  /**
   * Sets this group as default group or not
   *
   * @param defaultGroup whether the group should be the default group or not
   */
  void setDefaultGroup(boolean defaultGroup);

  /**
   * The sortId is used to sort users in the tablist. With a smaller sortId the user will show higher in tab. The sortId
   * has to have the same amount of digits as other sortIds to work properly
   *
   * @return the sortId of this group.
   */
  int getSortId();

  /**
   * Sets the sortId if this group
   *
   * @param sortId the sortId to set
   */
  void setSortId(int sortId);

  /**
   * @return the prefix of this group
   */
  String getPrefix();

  /**
   * Set the prefix of this group
   *
   * @param prefix the prefix to set
   */
  void setPrefix(@NotNull String prefix);

  /**
   * The color is used to color the name of players in 1.13+
   *
   * @return the color of this group
   */
  String getColor();

  /**
   * Sets the color of this group
   *
   * @param color the color to set
   */
  void setColor(@NotNull String color);


  String getSuffix();

  /**
   * Set the suffix of this group
   *
   * @param suffix the suffix to set
   */
  void setSuffix(@NotNull String suffix);

  /**
   * CloudNet-Chat sets this as prefix in the chat for users of this group
   *
   * @return the display of this group
   */
  String getDisplay();

  /**
   * Set the display of this group
   *
   * @param display the display to set
   */
  void setDisplay(@NotNull String display);

}
