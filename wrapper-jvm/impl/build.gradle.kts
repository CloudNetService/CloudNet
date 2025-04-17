import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  alias(libs.plugins.shadow)
}

val ignoredGroupIds = listOf("com.google.guava", "com.google.code.gson")
val exportCnlFile = tasks.register<ExportCnlFile>("exportCnlFile") {
  fileName = "wrapper.cnl"
  ignoredDependencyGroups = ignoredGroupIds
  setResolvedArtifacts(configurations.runtimeClasspath.get())
}
val exportLanguageFileInformation = tasks.register<ExportLanguageFileInformation>("exportLanguageFileInformation") {
  languageFiles.from(project.projectDir.resolve("src/main/resources/lang").listFiles())
}
tasks.shadowJar.configure {
  archiveFileName.set(Files.wrapper)

  // do not shade dependencies which we don't need to shade

  dependencies {
    exclude {
      it.moduleGroup != rootProject.group && !ignoredGroupIds.contains(it.moduleGroup)
    }
  }
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

  // google lib relocation
  relocate("com.google.gson", "eu.cloudnetservice.relocate.gson")
  relocate("com.google.common", "eu.cloudnetservice.relocate.guava")

  // drop unused classes which are making the jar bigger
  minimize()

  from(exportLanguageFileInformation)
  from(exportCnlFile)
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/wrapper.aero")
}

dependencies {
  "api"(projects.ext.modlauncher)
  "api"(projects.driver.driverApi)
  "api"(projects.driver.driverImpl)
  "api"(projects.wrapperJvm.wrapperJvmApi)
  "api"(projects.ext.platformInjectSupport.platformInjectLoader)

  // internal libraries
  "implementation"(libs.gson)
  "implementation"(libs.guava)
  "implementation"(libs.logbackCore)
  "implementation"(libs.logbackClassic)
  "implementation"(projects.utils.utilsBase)

  // processing
  "annotationProcessor"(libs.aerogelAuto)
  "annotationProcessor"(projects.driver.driverAp)
}

applyJarMetadata(
  "eu.cloudnetservice.wrapper.impl.Main",
  "eu.cloudnetservice.wrapper",
  "eu.cloudnetservice.wrapper.impl.Premain")
