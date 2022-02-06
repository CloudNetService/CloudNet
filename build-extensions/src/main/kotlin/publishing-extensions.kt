/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

fun Project.configurePublishing(publishedComponent: String, withJavadocAndSource: Boolean = false) {
  extensions.configure<PublishingExtension> {
    publications.apply {
      create("maven", MavenPublication::class.java).apply {
        from(components.getByName(publishedComponent))

        if (withJavadocAndSource) {
          artifact(tasks.getByName("sourcesJar"))
          artifact(tasks.getByName("javadocJar"))
        }

        pom.apply {
          name.set(project.name)
          description.set(project.description)
          url.set("https://cloudnetservice.eu")

          developers {
            developer {
              id.set("derklaro")
              email.set("git@derklaro.dev")
              timezone.set("Europe/Berlin")
            }
          }

          licenses {
            license {
              name.set("Apache License, Version 2.0")
              url.set("https://opensource.org/licenses/Apache-2.0")
            }
          }

          scm {
            tag.set("HEAD")
            url.set("git@github.com:CloudNetService/CloudNet-v3.git")
            connection.set("scm:git:git@github.com:CloudNetService/CloudNet-v3.git")
            developerConnection.set("scm:git:git@github.com:CloudNetService/CloudNet-v3.git")
          }

          issueManagement {
            system.set("GitHub Issues")
            url.set("https://github.com/CloudNetService/CloudNet-v3/issues")
          }

          ciManagement {
            system.set("GitHub Actions")
            url.set("https://github.com/CloudNetService/CloudNet-v3/actions")
          }

          withXml {
            val repositories = asNode().appendNode("repositories")
            project.repositories.forEach {
              if (it is MavenArtifactRepository && it.url.toString().startsWith("https://")) {
                val repo = repositories.appendNode("repository")
                repo.appendNode("id", it.name)
                repo.appendNode("url", it.url.toString())
              }
            }
          }
        }
      }
    }
  }

  extensions.configure<SigningExtension> {
    useGpgCmd()
    sign(extensions.getByType(PublishingExtension::class.java).publications.getByName("maven"))
  }

  tasks.withType<Sign> {
    onlyIf {
      !rootProject.version.toString().endsWith("-SNAPSHOT")
    }
  }

  // depend on the javadoc and sources task if requested
  if (withJavadocAndSource) {
    afterEvaluate {
      tasks.named("generateMetadataFileForMavenPublication") {
        dependsOn(tasks.withType(Jar::class.java))
      }

      // ugly hack to prevent gradle from whining
      tasks.findByName("shadowJar")?.run {
        configurations.getByName("runtimeClasspath").resolvedConfiguration.lenientConfiguration.allModuleDependencies
          .filter { it.moduleGroup.equals(this@configurePublishing.group) }
          .mapNotNull { tryFindProject(this@configurePublishing.gradle.rootProject, it.moduleName) }
          .forEach {
            dependsOn("${it.path}:javadocJar", "${it.path}:sourcesJar")
          }
      }
    }
  }
}

// hacky method to find a project by its name
fun tryFindProject(project: Project, name: String): Project? {
  // try to find the project
  val resolvedProject = project.subprojects.firstOrNull { it.name == name }
  if (resolvedProject != null) {
    return resolvedProject
  }

  // check subprojects of subprojects (if there are any)
  return project.subprojects.mapNotNull { tryFindProject(it, name) }.firstOrNull()
}
