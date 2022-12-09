repositories {
  maven("https://repo.md-5.net/repository/releases/")
  maven("https://repo.waterdog.dev/artifactory/main/")
  maven("https://repo.opencollab.dev/maven-snapshots/")
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
  "compileOnly"(projects.driver)
  "compileOnly"(libs.bundles.proxyPlatform)
  "compileOnly"(libs.bundles.serverPlatform)

  "api"(libs.javapoet)
  "api"(libs.bundles.nightConfig)
}
