/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  id("com.github.johnrengelman.shadow") version "7.1.0"
}

version = "2.0.0"

tasks.withType<Jar> {
  archiveFileName.set(Files.bridge)
}

repositories {
  mavenLocal()
}

dependencies {
  "compileOnly"(project(":cloudnet-wrapper-jvm"))
  "compileOnly"("cn.nukkit", "nukkit", Versions.nukkitX)
  "compileOnly"("org.spigotmc", "spigot-api", Versions.spigot)
  "compileOnly"("org.spongepowered", "spongeapi", Versions.sponge)
  "compileOnly"("net.md-5", "bungeecord-api", Versions.bungeecord)
  "compileOnly"("com.velocitypowered", "velocity-api", Versions.velocity)
  "compileOnly"("dev.waterdog.waterdogpe", "waterdog", Versions.waterdogpe)

  "annotationProcessor"("com.velocitypowered", "velocity-api", "3.1.11-SNAPSHOT")

  "implementation"("net.kyori", "adventure-api", Versions.adventure)
  "implementation"("net.kyori", "adventure-text-serializer-plain", Versions.adventure)
}

moduleJson {
  main = "de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule"
  author = "CloudNetService"
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development"
  runtimeModule = true
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
  // drop unused classes which are making the jar bigger
  minimize()
}

configure<net.kyori.blossom.BlossomExtension> {
  replaceToken("{project.build.version}", project.version)
}
