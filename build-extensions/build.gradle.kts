plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation("net.kyori", "indra-common", "3.0.1")
  implementation("com.google.code.gson", "gson", "2.10")
}
