# User management permission system for CloudNet Web

CloudNet has internal user management to secure certain operations via, for example, the REST API with access
privileges. The permission system or the service implementation can be changed at any time in the "registry" file. The
file is located by default in the /local directory. New ones can be added per module. Via the "perms" command, the
permissions of each user or role can be managed individually.

## User

You can create and remove users. The user needs a password and a default potency for permissions if the permission has
not enough potency itself.

```
perms create user Dytanic pw1234 100
```

You can add or delete permissions to users. You can also assign a role and / or group to the user.

## Role

A role can be assigned to one or more users to aggregate their permissions. Permissions can also be added, deleted or
managed.

```
perms create role Admin 100
```
