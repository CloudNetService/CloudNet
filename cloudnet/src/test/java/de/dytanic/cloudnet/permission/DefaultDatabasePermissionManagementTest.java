package de.dytanic.cloudnet.permission;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DefaultDatabasePermissionManagementTest {

    @Test
    public void testFilePermissionManager() throws Exception {
        String groupName = "Test", userName = "Tester", permission = "test.permission", groupPermission = "role.permission";
        AbstractDatabaseProvider databaseProvider = new H2DatabaseProvider("build/h2database", false);
        Assert.assertTrue(databaseProvider.init());

        System.setProperty("cloudnet.permissions.json.path", "build/group_permissions.json");

        ExecutorService executorService = Executors.newCachedThreadPool();

        IPermissionManagement permissionManagement = new DefaultDatabasePermissionManagement(() -> databaseProvider) {
            @Override
            public <V> ITask<V> scheduleTask(Callable<V> callable) {
                ITask<V> task = new ListenableTask<>(callable);
                executorService.execute(() -> {
                    try {
                        task.call();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
                return task;
            }
        };

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

    private void testRecursiveImplementations(IPermissionManagement permissionManagement, String groupName, String userName, String permission) {
        IPermissionGroup permissionGroup = permissionManagement.addGroup(groupName, 1);
        Assert.assertNotNull(permissionGroup);

        permissionGroup.getGroups().add(groupName);
        permissionManagement.updateGroup(permissionGroup);

        IPermissionUser permissionUser = permissionManagement.addUser(userName, "1234", 1);
        Assert.assertNotNull(permissionUser);

        permissionUser.addGroup(groupName);
        permissionManagement.updateUser(permissionUser);

        Assert.assertTrue(permissionUser.inGroup(groupName));

        StackOverflowError stackOverflowError = null;
        try {
            permissionManagement.hasPermission(permissionUser, permission);
        } catch (StackOverflowError error) {
            stackOverflowError = error;
        }

        Assert.assertNotNull(stackOverflowError);
        Assert.assertEquals(stackOverflowError.getMessage(), "Detected recursive permission group implementation on group " + groupName);

        permissionUser.removeGroup(groupName);
        permissionManagement.updateUser(permissionUser);

        try {
            permissionManagement.hasPermission(permissionUser, permission);
            stackOverflowError = null;
        } catch (StackOverflowError error) {
            stackOverflowError = error;
        }
        Assert.assertNull(stackOverflowError);


        permissionManagement.deleteUser(userName);
        permissionManagement.deleteGroup(groupName);
    }

}