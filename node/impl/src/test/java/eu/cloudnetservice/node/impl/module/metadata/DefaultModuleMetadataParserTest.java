/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module.metadata;

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.node.impl.junit.EnableServicesInject;
import eu.cloudnetservice.node.module.metadata.InvalidModuleMetadataException;
import eu.cloudnetservice.node.module.metadata.ModuleArtifact;
import eu.cloudnetservice.node.module.metadata.ModuleDependency;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@EnableServicesInject
public class DefaultModuleMetadataParserTest {

  @TempDir
  static Path tempDir;

  private static JarFile createTempJarFileWithManifest(String manifestContent) throws IOException {
    var randomFileName = UUID.randomUUID() + ".jar";
    var tempJarFilePath = tempDir.resolve(randomFileName);
    try (var jarOutputStream = new JarOutputStream(Files.newOutputStream(tempJarFilePath))) {
      var manifestEntry = new JarEntry("META-INF/cloudnet-module.json");
      jarOutputStream.putNextEntry(manifestEntry);
      jarOutputStream.write(manifestContent.getBytes(StandardCharsets.UTF_8));
      jarOutputStream.closeEntry();
    }

    return new JarFile(tempJarFilePath.toFile());
  }

  @Test
  void returnsNullIfTheSchemaIsNotSpecified() throws IOException {
    // language=json
    var manifest = """
      {
        "id": "eu.cloudnetservice.test",
        "name": "CloudNet-Test",
        "entrypoint": "eu.cloudnetservice.module.test.EntrypointClass"
      }
      """;
    var tempJar = createTempJarFileWithManifest(manifest);

    var parser = new DefaultModuleMetadataParser(DocumentFactory.json());
    var metadata = Assertions.assertDoesNotThrow(() -> parser.parseModuleMetadata(tempJar));
    Assertions.assertNull(metadata);
  }

  @Test
  void testSuccessfulManifestParsingWithOnlyRequiredFields() throws IOException {
    // language=json
    var manifest = """
      {
        "schema": "v1",
        "id": "eu.cloudnetservice.test",
        "name": "CloudNet-Test",
        "entrypoint": "eu.cloudnetservice.module.test.EntrypointClass"
      }
      """;
    var tempJar = createTempJarFileWithManifest(manifest);

    var parser = new DefaultModuleMetadataParser(DocumentFactory.json());
    var metadata = Assertions.assertDoesNotThrow(() -> parser.parseModuleMetadata(tempJar));
    Assertions.assertNotNull(metadata);
    Assertions.assertEquals("eu.cloudnetservice.test", metadata.id());
    Assertions.assertEquals("CloudNet-Test", metadata.displayName());
    Assertions.assertEquals("eu.cloudnetservice.module.test.EntrypointClass", metadata.entrypoint());

    Assertions.assertEquals("", metadata.description());
    Assertions.assertTrue(metadata.licenses().isEmpty());   // implicit null checks
    Assertions.assertTrue(metadata.artifacts().isEmpty());
    Assertions.assertTrue(metadata.moduleDependencies().isEmpty());
    Assertions.assertTrue(metadata.externalDependencies().isEmpty());
    Assertions.assertTrue(metadata.authors().isEmpty());
    Assertions.assertTrue(metadata.contributors().isEmpty());

    var version = metadata.version();
    Assertions.assertEquals(0, version.major());
    Assertions.assertEquals(0, version.minor());
    Assertions.assertEquals(0, version.patch());
  }

  @Test
  void testSuccessfulManifestParsingWithAllFields() throws IOException {
    // language=json
    var manifest = """
      {
        "schema": "v1",
        "id": "eu.cloudnetservice.test",
        "name": "CloudNet-Test",
        "entrypoint": "eu.cloudnetservice.module.test.EntrypointClass",
        "description": "Ye(e)t another test module!",
        "version": "4.2.5",
        "licenses": [
          "MIT",
          "Apache-2.0"
        ],
        "artifacts": [
          {
            "source": "FILESYSTEM",
            "sourcePath": "hello/world.jar",
            "targetPath": "plugins/world.jar",
            "environments": [
              "BUNGEECORD"
            ]
          }
        ],
        "dependencies": [
          {
            "id": "eu.cloudnetservice.bridge",
            "versionRange": ">=4.0",
            "dependencyType": "OPTIONAL"
          }
        ],
        "externalDependencies": [
          {
            "loader": "maven",
            "optional": true,
            "environments": [
              "node",
              "wrapper"
            ],
            "properties": {
              "groupId": "eu.cloudnetservice.cloudnet",
              "artifactId": "bridge-api"
            }
          }
        ],
        "authors": [
          {
            "name": "derklaro",
            "properties": {
              "discord": "@derklaro"
            }
          }
        ],
        "contributors": [
          {
            "name": "taginbert",
            "properties": {
              "discord": "@taschenzwerg"
            }
          }
        ],
        "properties": {
          "test": 1337
        }
      }
      """;
    var tempJar = createTempJarFileWithManifest(manifest);

    var parser = new DefaultModuleMetadataParser(DocumentFactory.json());
    var metadata = Assertions.assertDoesNotThrow(() -> parser.parseModuleMetadata(tempJar));
    Assertions.assertNotNull(metadata);
    Assertions.assertEquals("eu.cloudnetservice.test", metadata.id());
    Assertions.assertEquals("CloudNet-Test", metadata.displayName());
    Assertions.assertEquals("Ye(e)t another test module!", metadata.description());
    Assertions.assertEquals("eu.cloudnetservice.module.test.EntrypointClass", metadata.entrypoint());
    Assertions.assertEquals("4.2.5", metadata.version().displayString());

    var licenses = metadata.licenses();
    Assertions.assertEquals(2, licenses.size());
    Assertions.assertTrue(licenses.contains("MIT"));
    Assertions.assertTrue(licenses.contains("Apache-2.0"));

    Assertions.assertEquals(1, metadata.artifacts().size());
    var artifact = metadata.artifacts().iterator().next();
    Assertions.assertEquals(ModuleArtifact.Source.FILESYSTEM, artifact.source());
    Assertions.assertEquals("hello/world.jar", artifact.sourcePath());
    Assertions.assertEquals("plugins/world.jar", artifact.targetPath());
    Assertions.assertEquals(1, artifact.environments().size());
    Assertions.assertTrue(artifact.environments().contains("BUNGEECORD"));

    Assertions.assertEquals(1, metadata.moduleDependencies().size());
    var moduleDependency = metadata.moduleDependencies().iterator().next();
    Assertions.assertEquals("eu.cloudnetservice.bridge", moduleDependency.id());
    Assertions.assertEquals(">=4.0", moduleDependency.versionRange());
    Assertions.assertEquals(ModuleDependency.DependencyType.OPTIONAL, moduleDependency.type());

    Assertions.assertEquals(1, metadata.externalDependencies().size());
    var externalDependency = metadata.externalDependencies().iterator().next();
    Assertions.assertEquals("maven", externalDependency.loader());
    Assertions.assertTrue(externalDependency.optional());
    Assertions.assertEquals(2, externalDependency.environments().size());
    Assertions.assertTrue(externalDependency.environments().contains("node"));
    Assertions.assertTrue(externalDependency.environments().contains("wrapper"));
    Assertions.assertEquals("eu.cloudnetservice.cloudnet", externalDependency.propertyHolder().getString("groupId"));
    Assertions.assertEquals("bridge-api", externalDependency.propertyHolder().getString("artifactId"));

    Assertions.assertEquals(1, metadata.authors().size());
    var author = metadata.authors().iterator().next();
    Assertions.assertEquals("derklaro", author.name());
    Assertions.assertEquals("@derklaro", author.propertyHolder().getString("discord"));

    Assertions.assertEquals(1, metadata.contributors().size());
    var contributor = metadata.contributors().iterator().next();
    Assertions.assertEquals("taginbert", contributor.name());
    Assertions.assertEquals("@taschenzwerg", contributor.propertyHolder().getString("discord"));

    Assertions.assertEquals(1337, metadata.propertyHolder().getInt("test"));
  }

  @Test
  void testSemVerReleaseVersionIsParsedCorrectly() throws IOException {
    // language=json
    var manifest = """
      {
        "schema": "v1",
        "id": "eu.cloudnetservice.test",
        "name": "CloudNet-Test",
        "entrypoint": "eu.cloudnetservice.module.test.EntrypointClass",
        "version": "3.7.5+build.10637"
      }
      """;
    var tempJar = createTempJarFileWithManifest(manifest);

    var parser = new DefaultModuleMetadataParser(DocumentFactory.json());
    var metadata = Assertions.assertDoesNotThrow(() -> parser.parseModuleMetadata(tempJar));
    Assertions.assertNotNull(metadata);
    Assertions.assertEquals("eu.cloudnetservice.test", metadata.id());
    Assertions.assertEquals("CloudNet-Test", metadata.displayName());
    Assertions.assertEquals("eu.cloudnetservice.module.test.EntrypointClass", metadata.entrypoint());

    var version = metadata.version();
    Assertions.assertInstanceOf(SemVerModuleVersion.class, version);
    Assertions.assertEquals(3, version.major());
    Assertions.assertEquals(7, version.minor());
    Assertions.assertEquals(5, version.patch());
    Assertions.assertEquals("", version.preRelease());
    Assertions.assertEquals("build.10637", version.build());
    Assertions.assertEquals("3.7.5+build.10637", version.displayString());

    Assertions.assertTrue(version.satisfies("3.7.x"));
    Assertions.assertTrue(version.satisfies("^3.7.0"));
    Assertions.assertTrue(version.satisfies("~3.7.0"));
    Assertions.assertTrue(version.satisfies("3.7.0 - 3.7.9"));
    Assertions.assertTrue(version.satisfies(">=3.7.0 <3.8.0"));

    Assertions.assertFalse(version.satisfies("3.8.x"));
    Assertions.assertFalse(version.satisfies(">3.7.5"));
    Assertions.assertFalse(version.satisfies("<3.7.5"));
    Assertions.assertFalse(version.satisfies("^4.0.0"));
    Assertions.assertFalse(version.satisfies("3.7.6 - 3.7.9"));
  }

  @Test
  void testSemVerPreReleaseVersionIsParsedCorrectly() throws IOException {
    // language=json
    var manifest = """
      {
        "schema": "v1",
        "id": "eu.cloudnetservice.test",
        "name": "CloudNet-Test",
        "entrypoint": "eu.cloudnetservice.module.test.EntrypointClass",
        "version": "2.4.1-beta.3+build.20260318"
      }
      """;
    var tempJar = createTempJarFileWithManifest(manifest);

    var parser = new DefaultModuleMetadataParser(DocumentFactory.json());
    var metadata = Assertions.assertDoesNotThrow(() -> parser.parseModuleMetadata(tempJar));
    Assertions.assertNotNull(metadata);
    Assertions.assertEquals("eu.cloudnetservice.test", metadata.id());
    Assertions.assertEquals("CloudNet-Test", metadata.displayName());
    Assertions.assertEquals("eu.cloudnetservice.module.test.EntrypointClass", metadata.entrypoint());

    var version = metadata.version();
    Assertions.assertInstanceOf(SemVerModuleVersion.class, version);
    Assertions.assertEquals(2, version.major());
    Assertions.assertEquals(4, version.minor());
    Assertions.assertEquals(1, version.patch());
    Assertions.assertEquals("beta.3", version.preRelease());
    Assertions.assertEquals("build.20260318", version.build());
    Assertions.assertEquals("2.4.1-beta.3+build.20260318", version.displayString());

    Assertions.assertTrue(version.satisfies("2.4.1-beta.3"));
    Assertions.assertTrue(version.satisfies("~2.4.1-beta.1"));
    Assertions.assertTrue(version.satisfies("^2.4.1-beta.1"));
    Assertions.assertTrue(version.satisfies(">=2.4.1-beta.1 <2.4.1"));

    Assertions.assertFalse(version.satisfies("2.4.1"));
    Assertions.assertFalse(version.satisfies(">=2.4.1"));
    Assertions.assertFalse(version.satisfies("<2.4.1-beta.3"));
    Assertions.assertFalse(version.satisfies("~2.4.2-beta.1"));
    Assertions.assertFalse(version.satisfies("^3.0.0-beta.1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "cn", "Cloudnet.test", "CLOUDNET", "cloudnet:test", "000test", "cloudNet"})
  void testThrowsOnInvalidModuleId(String moduleId) throws IOException {
    // language=json
    var manifest = """
      {
        "schema": "v1",
        "id": "%s",
        "name": "CloudNet-Test",
        "entrypoint": "eu.cloudnetservice.module.test.EntrypointClass"
      }
      """.formatted(moduleId);
    var jar = createTempJarFileWithManifest(manifest);

    var parser = new DefaultModuleMetadataParser(DocumentFactory.json());
    var thrown = Assertions.assertThrows(InvalidModuleMetadataException.class, () -> parser.parseModuleMetadata(jar));
    Assertions.assertTrue(thrown.getMessage().contains("id"));
    Assertions.assertTrue(thrown.getMessage().contains("(val: '" + moduleId + "')"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "CN", "LOL", "CLOUDNET:TEST", ":::::"})
  void testThrowsOnInvalidModuleName(String moduleName) throws IOException {
    // language=json
    var manifest = """
      {
        "schema": "v1",
        "id": "eu.cloudnetservice.test",
        "name": "%s",
        "entrypoint": "eu.cloudnetservice.module.test.EntrypointClass"
      }
      """.formatted(moduleName);
    var jar = createTempJarFileWithManifest(manifest);

    var parser = new DefaultModuleMetadataParser(DocumentFactory.json());
    var thrown = Assertions.assertThrows(InvalidModuleMetadataException.class, () -> parser.parseModuleMetadata(jar));
    Assertions.assertTrue(thrown.getMessage().contains("name"));
    Assertions.assertTrue(thrown.getMessage().contains("(val: '" + moduleName + "')"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Hello\nWorld", "Test\r", "WTF?\u001B", "L\u0000L"})
  void testThrowsOnInvalidModuleDescription(String moduleDescription) throws IOException {
    // language=json
    var manifest = """
      {
        "schema": "v1",
        "id": "eu.cloudnetservice.test",
        "name": "CloudNet-Test",
        "description": "%s",
        "entrypoint": "eu.cloudnetservice.module.test.EntrypointClass"
      }
      """.formatted(moduleDescription);
    var jar = createTempJarFileWithManifest(manifest);

    var parser = new DefaultModuleMetadataParser(DocumentFactory.json());
    var thrown = Assertions.assertThrows(InvalidModuleMetadataException.class, () -> parser.parseModuleMetadata(jar));
    Assertions.assertTrue(thrown.getMessage().contains("description"));
    Assertions.assertTrue(thrown.getMessage().contains("(val: '" + moduleDescription + "')"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"hello.world..Test", "hello.world.Test-Class", "hello.world.Lol$2", "hello:test.world.XDD"})
  void testThrowsOnInvalidModuleEntrypoint(String moduleEntrypoint) throws IOException {
    // language=json
    var manifest = """
      {
        "schema": "v1",
        "id": "eu.cloudnetservice.test",
        "name": "CloudNet-Test",
        "entrypoint": "%s"
      }
      """.formatted(moduleEntrypoint);
    var jar = createTempJarFileWithManifest(manifest);

    var parser = new DefaultModuleMetadataParser(DocumentFactory.json());
    var thrown = Assertions.assertThrows(InvalidModuleMetadataException.class, () -> parser.parseModuleMetadata(jar));
    Assertions.assertTrue(thrown.getMessage().contains("entrypoint"));
    Assertions.assertTrue(thrown.getMessage().contains("(val: '" + moduleEntrypoint + "')"));
  }
}
