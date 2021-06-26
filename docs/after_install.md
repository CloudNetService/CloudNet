# After the installation

## Simple creation for a network

After the installation has been completed, you can now set up a simple network for Minecraft. This is shown for the
Minecraft version 1.8.8 with spigot as an example and BungeeCord

In this case the tasks "lobby" as **minecraft server** service, "bungee"
as **BungeeCord** service and a "test" task are created with the "tasks" command.

```
tasks create task Lobby minecraft_server
tasks create task Bungee bungeecord
tasks create task Test minecraft_server
```

To do this, the command creates a local template for each task with the task prefix and the name "default".

As an an example:
Lobby/default Test/default Bungee/default

For each task whose environment the configuration files have already been prepared. With the command "local-template
list" you can see all local templates. For each of these tasks, an additional **group** has been created so that you can
easily associate with the extensions.

You can either use the commands that automatically install Jar Archive, or the programs in the respective template
directory the "spigot.jar" or "bungee.jar"

```
local-template install minecraft_server
local-template install Lobby default minecraft_server spigot-1.8.8
local-template install Test default minecraft_server spigot-1.8.8
local-template install bungeecord
local-template install Bungee default bungeecord default
```

With the "tasks" command or in the /local/tasks.json file you can configure the tasks and groups, and add templates,
deployments and inlcusions there. If you use the configuration file, you should use the "reload config" command.

With the "create" command, you can now manually create a service based on a task. You can also create custom services
with "create new".

```
create by Bungee 1
create by Lobby 1

service Bungee-1 start
service Lobby-1 start
```

The "service" command allows the administration of the created service. The manual stop, start, restart and inform about
a service in the cluster can invoke with this command.

If the bridge module is installed, you can join the pre-configuration directly to the lobby server via the BungeeCord
server.

The further equipment is up to you!
Have fun!
