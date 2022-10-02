plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation("net.kyori", "indra-common", "2.2.0")
  implementation("com.google.code.gson", "gson", "2.9.1")
}
