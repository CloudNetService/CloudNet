package de.dytanic.cloudnet.permission;

import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

public class DefaultDatabasePermissionManagementTest {

    @Test
    public void testFilePermissionManager() throws Exception {
        String groupName = "Test", userName = "Tester", permission = "test.permission", groupPermission = "role.permission";

        new File("build/h2database.mv.db").delete();

        AbstractDatabaseProvider databaseProvider = new H2DatabaseProvider("build/h2database", false);
        Assert.assertTrue(databaseProvider.init());

        System.setProperty("cloudnet.permissions.json.path", "build/group_permissions.json");

        IPermissionManagement permissionManagement = new DefaultDatabasePermissionManagement(() -> databaseProvider);

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

        this.testRecursiveImplementations(permissionManagement, groupName, userName, permission);
    }

    private void testRecursiveImplementations(IPermissionManagement permissionManagement, String groupName, String userName, String permission) throws IOException {
        IPermissionGroup permissionGroup = permissionManagement.addGroup(groupName, 1);
        Assert.assertNotNull(permissionGroup);

        permissionGroup.getGroups().add(groupName);
        permissionManagement.updateGroup(permissionGroup);

        IPermissionUser permissionUser = permissionManagement.addUser(userName, "1234", 1);
        Assert.assertNotNull(permissionUser);

        permissionUser.addGroup(groupName);
        permissionManagement.updateUser(permissionUser);

        Assert.assertTrue(permissionUser.inGroup(groupName));

        PrintStream oldErr = System.err;
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errorStream));
        permissionManagement.hasPermission(permissionUser, permission);

        Assert.assertEquals(errorStream.toString(), "Detected recursive permission group implementation on group " + groupName + "\r\n");
        errorStream.reset();

        permissionUser.removeGroup(groupName);
        permissionManagement.updateUser(permissionUser);

        permissionManagement.hasPermission(permissionUser, permission);
        Assert.assertEquals(errorStream.toString(), "");

        permissionManagement.deleteUser(userName);
        permissionManagement.deleteGroup(groupName);

        System.setErr(oldErr);
    }

}