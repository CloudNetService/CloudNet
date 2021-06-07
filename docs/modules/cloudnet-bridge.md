### Bridge

The bridge module is the core of the modules, which are also plugins for the service. In addition it forms the
connection of the Minecraft servers to the proxy server and a Random Hub system with multiple fallbacks. To protect
against BungeeCord explode, this module also has extra login protection for the offline server. It provides the ingame
console dispatcher for BungeeCord with the / cloudnet (/ cloud) command. The permission for this is "
cloudnet.command.cloudnet"
This module currently supports the PluginManagers from Nukkit, Sponge, Bukkit and BungeeCord.

It is recommended to keep the module for the network, as it is supported by the amount of information and basic support
functions with the PluginManager of the services and for the own development of plugins and / or modules
