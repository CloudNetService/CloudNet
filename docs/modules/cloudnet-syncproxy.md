### SyncProxy

The SyncProxy module is responsible for the pure representation of the motd and the tab list in order to synchronize
proxy services within a group. She also has a maintenance mode with a whitelist on it. Above all, it is interesting when
it comes to the use of multiple proxy services to distribute the players there.

The permission, which you can join in maintenance mode

```
cloudnet.syncproxy.maintenance
```

Motd Patterns:

```
%proxy% - The current proxy service name
%proxy_uniqueId% - The current proxy service uniqueId
%task% - the current task name of the current proxy
%node% - the node uniqueId of the proxy
```

Tablist Patterns:

```
%proxy% - The current proxy service name
%proxy_uniqueId% - The current proxy service uniqueId
%server% - The server name which the player is connected
%task% - The task name of the server which the player is connected with
%online_players% - The current proxy online count
%max_players% - The max player size, which is configured is
%proxy_task_name% - The task from the proxy, which the player is connected
%name% - Name of the player which get the tab list
```
