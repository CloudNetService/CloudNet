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

package de.dytanic.cloudnet.permission;

import de.dytanic.cloudnet.EmptyCloudNetDriver;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class DefaultDatabasePermissionManagementTest {

  @Test
  public void testFilePermissionManager() throws Exception {
    String groupName = "Test";
    String otherGroupName = "Test2";
    String userName = "Tester";
    String permission = "test.permission";
    String permissionStar = "test.*";
    String groupPermission = "role.permission";

    FileUtils.delete(Paths.get("build/h2database.mv.db"));
    FileUtils.delete(Paths.get("build/group_permissions.json"));

    AbstractDatabaseProvider databaseProvider = new H2DatabaseProvider("build/h2database", false);
    Assert.assertTrue(databaseProvider.init());

    System.setProperty("cloudnet.permissions.json.path", "build/group_permissions.json");

    IPermissionManagement permissionManagement = new DefaultDatabasePermissionManagement(() -> databaseProvider);
    permissionManagement.init();

    CloudNetDriver driver = new EmptyCloudNetDriver() {{
      setInstance(this);
    }};
    driver.setPermissionManagement(permissionManagement);

    IPermissionUser permissionUser = permissionManagement.addUser(userName, "1234", (byte) 5);
    Assert.assertNotNull(permissionUser);
    Assert.assertEquals(1, permissionManagement.getUsers().size());

    permissionUser.addGroup(groupName);
    permissionManagement.updateUser(permissionUser);
    Assert.assertNotNull(permissionManagement.getUser(permissionUser.getUniqueId()));
    Assert.assertNotNull(permissionManagement.getUsers(permissionUser.getName()));
    Assert.assertFalse(permissionManagement.getUsers(permissionUser.getName()).isEmpty());

    permissionUser.addPermission(new Permission(permission, 10));
    permissionManagement.updateUser(permissionUser);
    Assert.assertNotNull(permissionManagement.getUser(permissionUser.getUniqueId()));
    Assert.assertTrue(permissionManagement.hasPermission(permissionUser, permission));
    Assert.assertFalse(permissionManagement.hasPermission(permissionUser, new Permission(permission, 11)));

    permissionUser.addPermission("Test_Group", new Permission(permission, 10));
    permissionManagement.updateUser(permissionUser);
    Assert.assertNotNull(permissionManagement.getUser(permissionUser.getUniqueId()));
    Assert.assertTrue(permissionManagement.hasPermission(permissionUser, "Test_Group", new Permission(permission)));
    Assert
      .assertFalse(permissionManagement.hasPermission(permissionUser, "Test_Group", new Permission(permission, 11)));

    permissionUser.addPermission("Test_Group", new Permission(permissionStar, -15));
    permissionManagement.updateUser(permissionUser);
    Assert.assertFalse(permissionManagement.hasPermission(permissionUser, "Test_Group", new Permission(permission)));

    permissionUser.addPermission("Test_Group", new Permission(permissionStar, 100));
    permissionManagement.updateUser(permissionUser);
    Assert.assertTrue(permissionManagement.hasPermission(permissionUser, "Test_Group", new Permission(permission)));
    Assert
      .assertFalse(permissionManagement.hasPermission(permissionUser, "Test_Group", new Permission(permission, 105)));

    permissionUser.addPermission(new Permission("test.permission.1234", 10, 5, TimeUnit.MILLISECONDS));
    Thread.sleep(10);
    Assert.assertTrue(permissionManagement.testPermissionUser(permissionUser));

    permissionManagement.updateUser(permissionUser);
    Assert.assertFalse(permissionManagement.hasPermission(permissionUser, "test.permission.1234"));

    permissionManagement.addGroup(groupName, (byte) 64);
    Assert.assertNotNull(permissionManagement.getGroup(groupName));
    Assert.assertEquals(1, permissionManagement.getGroups().size());

    IPermissionGroup permissionGroup = permissionManagement.getGroup(groupName);
    Assert.assertNotNull(permissionGroup);
    permissionGroup.addPermission(new Permission(groupPermission, 4));
    permissionManagement.updateGroup(permissionGroup);
    Assert.assertTrue(permissionManagement.hasPermission(permissionUser, groupPermission));
    Assert.assertFalse(permissionManagement.hasPermission(permissionUser, new Permission(groupPermission, 600)));

    permissionGroup.addPermission(new Permission("test.test.5678", 10, 1));
    permissionManagement.updateGroup(permissionGroup);
    Assert.assertFalse(permissionManagement.getGroup(groupName).hasPermission("test.test.5678").asBoolean());

    permissionGroup.addPermission("City", new Permission("test.test.5678", 10, 1));
    permissionManagement.updateGroup(permissionGroup);
    Assert.assertFalse(
      permissionManagement.getGroup(groupName).hasPermission("City", new Permission("test.test.5678")).asBoolean());

    permissionGroup.addPermission("City", new Permission("test.test.91011", -65));
    permissionManagement.updateGroup(permissionGroup);
    Assert.assertFalse(
      permissionManagement.getGroup(groupName).hasPermission("City", new Permission("test.test.91011")).asBoolean());

    permissionGroup.addPermission("Super_City", new Permission("test.perm.*", -10));
    permissionManagement.updateGroup(permissionGroup);
    Assert.assertFalse(
      permissionManagement.getGroup(groupName).hasPermission("Super_City", new Permission("test.perm.7859"))
        .asBoolean());

    permissionGroup.addPermission("Super_City", new Permission("test.perm.*", -100));
    permissionManagement.updateGroup(permissionGroup);
    Assert.assertFalse(
      permissionManagement.getGroup(groupName).hasPermission("Super_City", new Permission("test.perm.7859"))
        .asBoolean());

    permissionGroup.addPermission("Super_City", new Permission("test.perm.*", 101));
    permissionManagement.updateGroup(permissionGroup);
    Assert.assertTrue(
      permissionManagement.getGroup(groupName).hasPermission("Super_City", new Permission("test.perm.7859"))
        .asBoolean());
    Assert.assertFalse(
      permissionManagement.getGroup(groupName).hasPermission("Super_City", new Permission("test.perm.7859", 105))
        .asBoolean());

    permissionManagement.addGroup(otherGroupName, 100);
    Assert.assertNotNull(permissionManagement.getGroup(otherGroupName));
    Assert.assertEquals(2, permissionManagement.getGroups().size());

    IPermissionGroup otherPermissionGroup = permissionManagement.getGroup(otherGroupName);
    Assert.assertNotNull(otherPermissionGroup);
    otherPermissionGroup.addPermission(new Permission("test.peter.*", -1000));
    permissionManagement.updateGroup(otherPermissionGroup);

    permissionGroup.getGroups().add(otherGroupName);
    permissionGroup.addPermission(new Permission("test.peter.*", 999));
    permissionManagement.updateGroup(permissionGroup);

    Assert.assertFalse(permissionManagement.hasPermission(permissionGroup, new Permission("test.peter.7859")));
    Assert.assertFalse(permissionManagement.hasPermission(permissionUser, new Permission("test.peter.56565")));

    permissionGroup.addPermission(new Permission("test.peter.*", 1005));
    permissionManagement.updateGroup(permissionGroup);

    Assert.assertTrue(permissionManagement.hasPermission(permissionGroup, new Permission("test.peter.7859")));
    Assert.assertTrue(permissionManagement.hasPermission(permissionUser, new Permission("test.peter.56565")));

    Assert.assertEquals(1, permissionManagement.getUsersByGroup(groupName).size());
    permissionUser.removeGroup(groupName);
    permissionManagement.updateUser(permissionUser);
    Assert.assertFalse(permissionManagement.getUsers(permissionUser.getName()).get(0).inGroup(groupName));

    permissionManagement.deleteUser(userName);
    Assert.assertEquals(0, permissionManagement.getUsersByGroup(groupName).size());
    Assert.assertEquals(0, permissionManagement.getUsers().size());

    permissionManagement.deleteGroup(groupName);
    Assert.assertNull(permissionManagement.getGroup(groupName));
    Assert.assertTrue(permissionUser.checkPassword("1234"));
  }
}
