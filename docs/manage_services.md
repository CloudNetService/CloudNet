# Service Management

CloudNet, provides very extensive features to manage the services in the cluster. With the "create" command the services
can be created manually.

```
create new Lobby 1 minecraft_server templates=local:Lobby/default autoDeleteOnStop=false groups=Lobby,Global memory=356

or

create by Lobby 1
```

With the "--start" parameter, the registered and prepared services can automatically start automatically. The created
services can be managed in the cluster via the "service" command.

Show all services and some information from one Task

```
service list task=Lobby
```

Displayed, via the current information snapshots, you can via the UUID of the name. It is enough to write only a part of
the respective one. The info parameter also displays the plug-in and module information, which is transmitted via the
bridge module in JSON.

```
service Lobby-1
service 32bc1b46-a8ac-40cf-9fea-208984c61c7e

service Lobby-1 info
service 32bc1b46-a8ac-40cf-9fea-208984c61c7e info
```

Each service can be easily started, stopped, restarted and deleted.

```
service Lobb start
service Lobby stop
service Lobby-1 stop --force //For
service 32bc delete
service L restart
```

You can run a started service with the "command" parameter in the console.

```
service Lobby-1 command say Hello Server!
```

In addition, you can additionally reboot for a server, adding templates and inclusions to a service. And if the service
is deleted, you can add additional deployments.

```
service Lobby-1 add template local Lobby global
service Lobby-1 add inclusion https://hub.spigotmc.org/jenkins/job/spigot-essentials/10/artifact/Essentials/target/Essentials-2.x-SNAPSHOT.jar plugins/essentials.jar
service Lobby-1 restart
service Lobby-1 add deployment local Lobby SavedLobby
```

You can make persistable (static) services from one task to add a deployment

```
tasks task Lobby add deployment local Lobby default
```

or

as static service task. Important is, the templates, inclusions and deployments will execute, too. You should remove the
configuration for the templates, or the templates has to be empty

```
tasks task Lobby set static true
```

With the "screen" command, you allow the node to output the console output that is stored between and is output live
from the application

```
screen Lobby-1
```
