# Modules

Here is an introduction to the modules. These modules are the standard equipment. These modules include the advanced
functionality of CloudNet. They should be a support for a Minecraft network to save a lot of their own technical
developments or reduce the effort. All standard modules that have a configuration have a config.json in each module's
name folder.

### Bridge

The bridge module is the core of the modules, which are also plugins for the service. In addition it forms the
connection of the Minecraft servers to the proxy server and a Random Hub system with multiple fallbacks. To protect
against BungeeCord explode, this module also has extra login protection for the offline server. It provides the ingame
console dispatcher for BungeeCord with the / cloudnet (/ cloud) command. The permission for this is "
cloudnet.command.cloudnet"
This module currently supports the PluginManagers from Nukkit, Sponge, Bukkit and BungeeCord.

It is recommended to keep the module for the network, as it is supported by the amount of information and basic support
functions with the PluginManager of the services and for the own development of plugins and / or modules

### Signs

The sign system has a sorted animated, live updating sign system, depending on the order of occupation. The tags can
even be targeted to specific groups with a template path limitation. The services as well as the sign system itself need
the CloudNet bridge module, so that the information for the presentation is available. Otherwise, the signs would only
show the servers as the starting phase. With the /cloudsign command, the signs can be set or deleted while watching one
of them.

Here is a list of patterns for the sign layout

```
%task% - Name of task
%task_id% - Current id from one task
%group% - The group which the sign has target
%name% - The readable Name of the service like "BedWars-1"
%uuid% - The first characters of the service uniqueId
%node% - The node from the service
%environment% - The environment type of the service which is configured
%life_cycle% - The current life_cycle type
%runtime% - The runtime process type
%port% - The current configured port of the service
%cpu_usage% - The current CPU usage of the service
%threads% - The current Thread count
%online% - Is the service online or not from the "Online" property in Bridge-Module
%online_players% - The current online count of the service
%max_players% - The max players size from the service
%motd% - The Motd of the service or the Text of the CloudNet-BridgeAPI
%extra% - The extra string text by the CloudNet-Bridge API
%state% - The state string text by the CloudNet-Bridge API
%version% - The current version of the service
%whitelist% - Request if the whitelist enabled or not
```

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
%online_players% - The current proxy online count
%max_players% - The max player size, which is configured is
%proxy_task_name% - The task from the proxy, which the player is connected
%name% - Name of the player which get the tab list
```

### Smart

The Smart Module adds configurations to the actual Task Configurations, plus additional customizations adapted to the
CloudNet Bridge Module. Through this, processes that exist in CloudNet can be optimized for a MiniGame network so that
the need for game servers is optimally adapted to those of the players.

### Cloudflare

The Cloudflare Module allows automatic entry of Domain DNSRecords for larger required network capacities. These can be
made from any type of application, from bungee cord to normal vanilla servers, the group sets the tone. For the
configuration you need a domain and an account at the provider https://cloudflare.com. Several domains with different
users etc. can be managed, even for the same groups. You need the email, the API key from the account (not the global
API key!), The ZoneId found on the domain's dashboard page.

The "@" wildcard in the "sub" configuration of a group determines that it does not become a subdomain that owns a
third-level domain, but only a second-level domain. The configuration must be set up in the cluster at each node
individually, so that one can avoid unintentional entries such as nodes that are within an internal LAN and can only be
reached via specific ports or proxy servers.

### Storage FTP/FTPS

The module allows a TemplateStorage to connect to a remote server with either FTP or FTPS. For this, the current file
path is still required in the configuration. The structure of the templates is similar to that of the Local Template
System, except that the default storage name is "ftp". However, this can be changed in the config.json of this module.
Basically, the module needs a proper basic configuration to use it. The use of the template storage "ftp" could lead to
immediate errors without previous configuration.

### Report Module

The Report Module allows for easy output with the help of the "report" command, all the information that is relevant to
developers at modules or the cloud. In addition, this module stores all important information and log files of every
service that exists in the modules/CloudNet-Report/records directory. It can be changed at any time in the configuration
file of the module.

### Database MySQL

This module allows an alternative database system for the internal staticServices of data. MySQL and MariaDB are used as
alternative databases. In the "registry" file, you can use the database system on "mysql" or what is set in the
configuration file and restart the cloud. This module can only be loaded once at runtime. It can not be reloaded
manually except through the Node API.
