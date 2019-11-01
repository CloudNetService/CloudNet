package de.dytanic.cloudnet.driver.permission;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class DefaultJsonFilePermissionManagementTest {

    @Test
    public void testFilePermissionManager() throws Exception {
        String groupName = "Test", userName = "Tester", permission = "test.permission", groupPermission = "role.permission";
        File permissionsConfig = new File("build/permissions.json");

        IPermissionManagement permissionManagement = new DefaultJsonFilePermissionManagement(permissionsConfig);

        IPermissionUser permissionUser = permissionManagement.addUser(userName, "1234", (byte) 5);
        Assert.assertNotNull(permissionUser);
        Assert.assertEquals(1, permissionManagement.getUsers().size());

        permissionUser.addGroup(groupName);
        permissionManagement.updateUser(permissionUser);
        Assert.assertNotNull(permissionManagement.getUser(permissionUser.getUniqueId()));
        Assert.assertNotNull(permissionManagement.getUsers(permissionUser.getName()));

        permissionUser.addPermission(new Permission(permission, 10));
        permissionManagement.updateUser(permissionUser);
        Assert.assertNotNull(permissionManagement.getUser(permissionUser.getUniqueId()));
        Assert.assertTrue(permissionManagement.hasPermission(permissionUser, permission));
        Assert.assertFalse(permissionManagement.hasPermission(permissionUser, new Permission(permission, 11)));

        permissionUser.addPermission("Test_Group", new Permission(permission, 10));
        permissionManagement.updateUser(permissionUser);
        Assert.assertNotNull(permissionManagement.getUser(permissionUser.getUniqueId()));
        Assert.assertTrue(permissionManagement.hasPermission(permissionUser, "Test_Group", new Permission(permission)));
        Assert.assertFalse(permissionManagement.hasPermission(permissionUser, "Test_Group", new Permission(permission, 11)));

        permissionUser.addPermission(new Permission("test.permission.1234", 10, 5, TimeUnit.MILLISECONDS));
        Thread.sleep(10);
        Assert.assertTrue(permissionManagement.testPermissionUser(permissionUser));

        permissionManagement.updateUser(permissionUser);
        Assert.assertFalse(permissionManagement.hasPermission(permissionUser, "test.permission.1234"));

        permissionManagement.addGroup(groupName, (byte) 64);
        Assert.assertNotNull(permissionManagement.getGroup(groupName));
        Assert.assertEquals(1, permissionManagement.getGroups().size());

        IPermissionGroup permissionGroup = permissionManagement.getGroup(groupName);
        permissionGroup.addPermission(new Permission(groupPermission, 4));
        permissionManagement.updateGroup(permissionGroup);
        Assert.assertTrue(permissionManagement.hasPermission(permissionUser, groupPermission));
        Assert.assertFalse(permissionManagement.hasPermission(permissionUser, new Permission(groupPermission, 600)));

        permissionGroup.addPermission(new Permission("test.test.5678", 10, 1));
        permissionManagement.updateGroup(permissionGroup);
        Assert.assertFalse(permissionManagement.getGroup(groupName).hasPermission("test.test.5678").asBoolean());

        permissionGroup.addPermission("City", new Permission("test.test.5678", 10, 1));
        permissionManagement.updateGroup(permissionGroup);
        Assert.assertFalse(permissionManagement.getGroup(groupName).hasPermission("City", new Permission("test.test.5678")).asBoolean());

        permissionGroup.addPermission("City", new Permission("test.test.91011", -1));
        permissionManagement.updateGroup(permissionGroup);
        Assert.assertFalse(permissionManagement.getGroup(groupName).hasPermission("City", new Permission("test.test.91011")).asBoolean());

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