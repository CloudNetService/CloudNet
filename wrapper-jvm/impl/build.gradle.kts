import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  alias(libs.plugins.shadow)
}

tasks.withType<ShadowJar> {
  archiveFileName.set(Files.wrapper)
  archiveVersion.set(null as String?)

  // do not shade dependencies which we don't need to shade
  val ignoredGroupIds = arrayOf("com.google.guava", "com.google.code.gson")
  dependencies {
    exclude {
      it.moduleGroup != rootProject.group && !ignoredGroupIds.contains(it.moduleGroup)
    }
  }

  // google lib relocation
  relocate("com.google.gson", "eu.cloudnetservice.relocate.gson")
  relocate("com.google.common", "eu.cloudnetservice.relocate.guava")

  // drop unused classes which are making the jar bigger
  minimize()

  doFirst {
    // Note: included dependencies will not be resolved, they must be available from the node resolution already
    from(exportLanguageFileInformation())
    from(exportCnlFile("wrapper.cnl", ignoredGroupIds))
  }
}

tasks.withType<JavaCompile> {
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
