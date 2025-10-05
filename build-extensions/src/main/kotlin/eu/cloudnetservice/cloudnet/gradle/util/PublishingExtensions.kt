/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.gradle.util

import eu.cloudnetservice.cloudnet.gradle.plugins.JAVA_CORE_COMPATIBILITY
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.JavadocMemberLevel
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

// map of link (must end with trailing slash) -> cache directory name; links are applied in CloudNetPlugin
val EXTERNAL_JAVADOC_LINKS = mapOf(
  "https://projectlombok.org/api/" to "lombok",
  "https://jd.advntr.dev/api/latest/" to "adventure",
  "https://javadoc.io/doc/io.vavr/vavr/0.10.7/" to "vavr",
  "https://javadoc.io/doc/org.incendo/cloud-core/latest/" to "cloud-core",
  "https://javadoc.io/doc/org.jetbrains/annotations/latest/" to "jb-annotations",
  "https://javadoc.io/doc/dev.derklaro.aerogel/aerogel/latest/" to "aerogel",
  "https://docs.oracle.com/en/java/javase/${JAVA_CORE_COMPATIBILITY.asInt()}/docs/api/" to "java"
)

fun Project.configurePublishing(publishedComponent: String) {
  extensions.configure<PublishingExtension> {
    // pull this out because of configuration cache
    val projectName = project.name
    val projectDescription = project.description

    data class Repository(val id: String, val url: String)

    val selectedRepositories = project.repositories.filterIsInstance<MavenArtifactRepository>().filter {
      it.url.scheme == "https"
    }.map {
      Repository(it.name, it.url.toString())
    }

    publications.apply {
      register<MavenPublication>("maven") {
        from(components.getByName(publishedComponent))

        pom.apply {
          name.set(projectName)
          description.set(projectDescription)
          url.set("https://cloudnetservice.eu")

          developers {
            developer {
              id.set("derklaro")
              email.set("me@derklaro.dev")
              timezone.set("Europe/Berlin")
            }

            developer {
              id.set("0utplay")
              email.set("me@0utplay.de")
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
            url.set("git@github.com:CloudNetService/CloudNet.git")
            connection.set("scm:git:git@github.com:CloudNetService/CloudNet.git")
            developerConnection.set("scm:git:git@github.com:CloudNetService/CloudNet.git")
          }

          issueManagement {
            system.set("GitHub Issues")
            url.set("https://github.com/CloudNetService/CloudNet/issues")
          }

          ciManagement {
            system.set("GitHub Actions")
            url.set("https://github.com/CloudNetService/CloudNet/actions")
          }

          withXml {
            val repositories = asNode().appendNode("repositories")
            selectedRepositories.forEach {
              val repo = repositories.appendNode("repository")
              repo.appendNode("id", it.id)
              repo.appendNode("url", it.url)
            }
          }
        }
      }
    }
  }

  extensions.configure<SigningExtension> {
    val signingPrivateKey = System.getenv("SIGNING_KEY")
    val signingPrivateKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")

    // can only use the in-memory provider if both values are present (running on ci)
    if (signingPrivateKey != null && signingPrivateKeyPassword != null) {
      useInMemoryPgpKeys(signingPrivateKey, signingPrivateKeyPassword)
    } else {
      useGpgCmd()
    }

    sign(extensions.getByType(PublishingExtension::class.java).publications.getByName("maven"))
  }

  val version = this.version
  tasks.withType<Sign>().configureEach {
    onlyIf {
      !version.toString().endsWith("-SNAPSHOT")
    }
  }

  plugins.withId("java") {
    extensions.configure<JavaPluginExtension> {
      // when we publish a component, we also want sources and javadoc
      withSourcesJar()
      withJavadocJar()
    }
    tasks.withType<Javadoc>().configureEach {
      val options = options as? StandardJavadocDocletOptions ?: return@configureEach
      applyJavadocOptions(options)
    }
  }
}

fun applyJavadocOptions(options: StandardJavadocDocletOptions) {
  options.use()
  options.locale = "en"
  options.encoding = "UTF-8"
  options.memberLevel = JavadocMemberLevel.PRIVATE
  options.addBooleanOption("quiet", true)
  options.addBooleanOption("-enable-preview", true)
  options.addBooleanOption("Xdoclint:all,-missing", true)
  options.addStringOption("-link-modularity-mismatch", "info")
}
