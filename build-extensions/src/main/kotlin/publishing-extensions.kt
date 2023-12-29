/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.external.javadoc.JavadocMemberLevel
import org.gradle.external.javadoc.StandardJavadocDocletOptions
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
            mapOf(
              "0utplay" to "Aldin Sijamhodzic",
              "derklaro" to "Pasqual Koschmieder",
            ).forEach {
              developer {
                id.set(it.key)
                name.set(it.value)
                url.set("https://github.com/${it.key}")
              }
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

  tasks.withType<Sign> {
    onlyIf {
      !rootProject.version.toString().endsWith("-SNAPSHOT")
    }
  }
}

fun applyDefaultJavadocOptions(options: StandardJavadocDocletOptions, targetJavaVersion: JavaVersion) {
  options.use()
  options.encoding = "UTF-8"
  options.memberLevel = JavadocMemberLevel.PRIVATE
  options.addBooleanOption("Xdoclint:all,-missing", true)
  options.addStringOption("-release", targetJavaVersion.majorVersion)
  options.links(
    "https://projectlombok.org/api/",
    "https://jd.adventure.kyori.net/api/4.11.0/",
    "https://javadoc.io/doc/com.konghq/unirest-java/latest/",
    "https://javadoc.io/doc/org.jetbrains/annotations/latest/",
    "https://javadoc.io/doc/cloud.commandframework/cloud-core/latest/"
  )
}
