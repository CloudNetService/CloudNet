# Components

### Launcher

The launcher is the program that enables the preparation of the actual application. It can automatically update CloudNet
in release versions. He also manages the versions and dependencies of a version. This prevents redundancy of libraries
and increases the startup speed of new versions. Furthermore, the standard modules can always be updated for the current
launcher version. The launcher interprets the .cnl files to provide all major system configurations for the application.
Opposite the config.json is the launcher.cnl file. The launcher.cnl configuration, are also provided as system
properties during the actual runtime of the application.

The version, which is by default set to "default", can be switched to a specific version. For example, on the one in the
folder launcher / versions.

### Services (Cloudservices)

Any Minecraft / Proxy server etc. that can be created by CloudNet is considered a service of everything on the network.
This state is so abstracted that with CloudNet it is easy to integrate many programs like Nukkit, Glowstone, Velocity or
others. The fact that the services are created via an application wrapper, and then the actual programs are loaded at
runtime, resulting in a high degree of preparation and integration. The services are also not distinguished, except for
the types of service, to allow customized configurations, such as setting the port for the server or changing certain
configurations.

Services can be created with CloudNet in many different ways, including the "create" command. Every service is based on
a task. For example, if the "create" command creates a completely new server named "test", it will be based on the
task "test" and will also receive the task service id 1. "test-1" would be the first service from the task "test".

Then this would be about a task configuration that you can create and provides a rough configuration form for a service.

The services are always temporary, but there is also the configuration "autoDeleteOnStop", which aggravates the factor
so that the service is automatically deleted when it stops. Otherwise, the services can be started, stopped and changed
as desired and new templates added or inclusions of web pages.

Saved, these services can be called "deployments". They allow you to write back the entire service to a TemplateStorage
service such as Local.

### Tasks

Tasks are activities CloudNet should perform. Each service is based on one of these tasks. Each task can own groups.
Each task forms the "skeleton"
of a service and its inclusions, templates, deployments, group affiliation etc.

Tasks are also used to identify the individual services running on the network. However, apart from the type of service,
they do not exactly describe which properties the individual application type possesses.

Tasks can be managed with the "tasks" command in CloudNet or in the "tasks.json" in the "local/" directory

```
tasks create task Lobby minecraft_server
tasks create task Bungee bungeecord
tasks create task LobbyNukkit nukkit
tasks create task MapServer glowstone
```

### Multi groups

The groups are intended to summarize services and tasks. Each service can belong to one task and several groups. Groups
can add their own extra settings to the tasks as well as other groups, inclusions, deployments, templates.

As an example, there is the group lobby, the task "Lobby" and the task "PremiumLobby". Both tasks have membership of the
group "Lobby". In its configuration, the Lobby group determines that all tasks belonging to the "Lobby" group should
also retrieve the templates local with the prefix/folder "Lobby" and the name "all" and install them in the service.

Groups can be used by developers to extend the range of possibilities and abstraction of tasks. There may be different
types of tasks in a group, such as "Lobby" and "Bungee" in the "Global" group.

### Template storage

With the API, CloudNet allows the use of multiple template storage services to retrieve or deploy templates. By default,
the core of CloudNet brings the "local" template storage service.

Every service template that can be configured has a storage, ie the service from which the data is to be retrieved or
transmitted. The prefix is to set the namespace of the template in order to combine several templates within one
namespace. The name is the actual name of the template.

```json
        {
          "prefix": "Lobby",
          "name": "default",
          "storage": "local"
        }
```

For each task, which is created with the "tasks" command, a local template is also created. The local templates are
always synchronized in the cluster.
