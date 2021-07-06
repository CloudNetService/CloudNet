# Cloud Network Environment Technology 3 for Minecraft

![CloudNet V3 Logo](./docs/images/header.png)

## What is CloudNet?

**CloudNet** is an alternative application that can dynamically and easy deploy Minecraft oriented software. It should
greatly simplify the work and the technical processes within a Minecraft server network or with standalone servers. The
program should be the basis for a **Minecraft network**. With a **very extensive** API and a **very modular**
architecture, the program should be easily extensible in all its capabilities. It should be a solution to the most
creative ideas that brighten our Minecraft world. From minigame networks, CityBuild servers to private servers with
CloudNet is the work **low**. If it needs to be developed for CloudNet, it will provide an **API** (Driver) that can be
used to develop Bukkit / Sponge / Nukkit plugins or to develop modules to extend the core system. The **application
wrapper** for the **JVM** allows support for a wide variety of Minecraft server software and allows direct inclusion of
the API to retain it throughout the lifetime of the application.

###### **CloudNet v3** is the next generation of Minecraft Java and Bedrock cloud systems

## Features

- **Plug&Play**
- **Lightweight runtime launcher** for multiple versions or easy developing on CloudNet
- Installer of dependencies
- **Module** system with default modules
- User management system with roles
- **Easy server management** with simple commands
- Tasks to classify the services
- Internal database system based on H2
- **Multi group** and configuration
- Dynamic Template Storage System
- Dynamic Deployment System
- Dynamic File inclusions for services with HTTP/HTTPS support
- **Live** updating and working in a cluster
- Smart service instance deployments in cluster
- Supports static services for projects like CityBuild or Vanilla Minecraft
- Automatic error log printer from running services into node console
- New **non blocking service console log caching** without BungeeCord timeouts and infinity console streams
- A fast **HTTP/HTTPS server**
- A **RESTful** API
- **SSL/TLS support** for security connections between the nodes in cluster or between the services and the node with or
  none own certificates
- Multi system support and synchronizing in a network cluster
- Support for Minecraft **vanilla 1.0+**
- Application support for [Nukkit Project for Bedrock Edition 1.7+](https://github.com/CloudburstMC/Nukkit)
- Application support for [Bukkit based Minecraft **
  1.8.8+** (Spigot, PaperSpigot and more...)](https://github.com/Bukkit/Bukkit)
- Application support for [Sponge based Minecraft servers with the SpongeAPI **7.0.0+**](https://www.spongepowered.org/)
- Application support for [BungeeCord proxy server and forks for MC **1.8.8+**](https://github.com/SpigotMC/BungeeCord)
- Application support for [GlowStone Minecraft server for MC **1.8.9+**](https://glowstone.net)
- Application support for [Velocity Minecraft Java edition proxy server](https://www.velocitypowered.com)
- Application support for [Waterdog Minecraft Bedrock Edition proxy server](https://github.com/yesdog/Waterdog)
- Application support for [GoMint Minecraft Bedrock Edition server software](https://github.com/GoMint/GoMint)
- A **really big API** for **asynchronously** programming or **synchronously** programming
- A **Bridge module**, which includes the basics for the Bukkit, Sponge, BungeeCord and Nukkit API and for BungeeCord a
  /cloudnet command to dispatch the console of CloudNet ingame
- **BungeeCord exploit protection** with the Bridge Module for BungeeCord MC **1.8.8+** and Velocity
- **Random Hub** and **/hub** command with the Bridge Module for BungeeCord MC **1.8.8+** and Velocity
- **/cloudnet** command which dispatches the console of CloudNet Ingame for BungeeCord MC **1.8.8+** and Velocity
- A live, ingame, sorted signs system for Bukkit and Sponge with a dynamic animation and configuration for each group.
- A **SyncProxy module**, which include the **synchronization between two or more BungeeCord services** in one group.
- **Motd** layout configuration and synchronizing between Proxys with **SyncProxy module**
- **Animated Tablist** configuration with **SyncProxy module**
- A **Permissions module** which integrate the CloudNet user permissions system into the services plugin managers
- A **Smart module** for advanced configurations and automatic task management
- A **Cloudflare module** for dynamic adding and removing of DNS records for multi BungeeCord services
- A **Report module** Easily create reports to hear the services or node in the cluster and provide easy support.
- A **Storage FTP/FTPS Module** to transport templates from an external server via FTP / FTPS
- A **MySQL/MariaDB database support module** as alternative central database for CloudNet data by other modules
- Memory management protection
- CPU management protection

## Minimal requirements

- Java 8 JRE (alt. 10 or 11)
- 128MB JVM Heap size
- 2GB DDR3 System memory
- 2 virtual cores
- Internet connection

## Recommended requirements

- Java 11 JRE
- 512MB JVM Heap size
- 8GB DDR3 system memory
- 2-4 virtual cores
- Internet connection

## Build

Linux / OSX

```
git clone https://github.com/CloudNetService/CloudNet-v3.git
cd CloudNet-v3
./gradlew
```

Windows

```
git clone https://github.com/CloudNetService/CloudNet-v3.git
cd CloudNet-v3
gradlew.bat
```

## Setup

CloudNet should be started via the following script via Shell.

#### Default:

```
java -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:CompileThreshold=100 -XX:+UnlockExperimentalVMOptions -XX:+UseCompressedOops -Xmx512m -Xms256m -jar launcher.jar
```

The launcher should install the application files, then the dependencies. If it is finished, it will run the application
itself. For each version, this preparation is done dynamically.

**Try it out!**
