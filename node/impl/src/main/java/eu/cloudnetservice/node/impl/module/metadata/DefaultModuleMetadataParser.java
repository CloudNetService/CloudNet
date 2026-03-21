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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.module.dependency.ModuleExternalDependency;
import eu.cloudnetservice.node.module.metadata.InvalidModuleMetadataException;
import eu.cloudnetservice.node.module.metadata.ModuleArtifact;
import eu.cloudnetservice.node.module.metadata.ModuleContributor;
import eu.cloudnetservice.node.module.metadata.ModuleDependency;
import eu.cloudnetservice.node.module.metadata.ModuleMetadata;
import eu.cloudnetservice.node.module.metadata.ModuleMetadataParser;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Default module metadata parser for a module. Resolves the metadata JSON at the path
 * {@code META-INF/cloudnet-module.json} within the module file, parses the JSON content and extracts all the metadata
 * information from it.
 *
 * @since 4.0
 */
@Singleton
@AutoService(services = ModuleMetadataParser.class, name = "default", markAsDefault = true)
public final class DefaultModuleMetadataParser implements ModuleMetadataParser {

  private static final Pattern ID_PATTERN = Pattern.compile("^[a-z][a-z0-9-.]{3,63}$");
  private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}-_]{4,64}$");
  private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^[^\\p{Cntrl}<>]{0,128}$");
  private static final Pattern ENTRYPOINT_PATTERN = Pattern.compile("^([a-zA-Z0-9_]+\\.)*[A-Za-z0-9_]*$");

  private static final Type LIST_STRING_TYPE = TypeFactory.parameterizedClass(List.class, String.class);
  private static final Type LIST_DOCUMENT_TYPE = TypeFactory.parameterizedClass(List.class, Document.class);

  private static final String SCHEMA_V1 = "v1";

  private final DocumentFactory jsonDocumentFactory;

  @Inject
  public DefaultModuleMetadataParser(@NonNull @Service(name = "json") DocumentFactory jsonDocumentFactory) {
    this.jsonDocumentFactory = jsonDocumentFactory;
  }

  /**
   * Extracts a string from the given document source, throwing an exception if the string is not matching the given
   * validation pattern. This method extracts an empty string from the document if the key is not present, it is up to
   * the validator pattern to reject empty strings if they are undesired.
   *
   * @param source    the document to extract the key value from.
   * @param sourceKey the key of the element to get from the document.
   * @param validator the validator pattern that the value must pass.
   * @return a string from the given document that matches the given validation pattern.
   * @throws NullPointerException           if one of the given parameters is null.
   * @throws InvalidModuleMetadataException if the string value does not match the given validator pattern.
   */
  private static @NonNull String extractValidString(
    @NonNull Document source,
    @NonNull String sourceKey,
    @NonNull Pattern validator
  ) {
    var value = source.getString(sourceKey, "");
    var valueMatcher = validator.matcher(value);
    if (valueMatcher.matches()) {
      return value;
    }

    throw new InvalidModuleMetadataException("module metadata key " + sourceKey + " (val: '" + value + "') is invalid");
  }

  /**
   * Checks that the given value is not null or empty. Currently checks for emptiness of strings and collections.
   *
   * @param value   the value to check.
   * @param message the message to use for the exception if the given value is null or empty.
   * @param <T>     the type of the value.
   * @throws NullPointerException           if the given message is null.
   * @throws InvalidModuleMetadataException if the given value is null or empty.
   */
  private static <T> void checkNotNullOrEmpty(@Nullable T value, @NonNull String message) {
    switch (value) {
      case null -> throw new InvalidModuleMetadataException(message);
      case String str when str.isBlank() -> throw new InvalidModuleMetadataException(message);
      case Collection<?> col when col.isEmpty() -> throw new InvalidModuleMetadataException(message);
      default -> {
        // ignore, everything is fine
      }
    }
  }

  /**
   * Parses the declared module artifacts in the given module metadata document.
   *
   * @param metadataJson the metadata document to parse the artifacts from.
   * @return the parsed module artifacts from the given metadata document.
   * @throws NullPointerException           if the given metadata document is null.
   * @throws InvalidModuleMetadataException if an invalid value is encountered while parsing.
   */
  @NonNull
  @Unmodifiable
  private static Collection<ModuleArtifact> parseModuleArtifacts(@NonNull Document metadataJson) {
    var artifactsDocuments = metadataJson.readObject("artifacts", LIST_DOCUMENT_TYPE, List.<Document>of());
    return artifactsDocuments.stream()
      .map(artifactDocument -> {
        var source = artifactDocument.readObject(
          "source",
          ModuleArtifact.Source.class,
          ModuleArtifact.Source.CLASSPATH);
        var sourcePath = artifactDocument.getString("sourcePath");
        var targetPath = artifactDocument.getString("targetPath");
        var environments = artifactDocument.readObject("environments", LIST_STRING_TYPE, List.<String>of());

        // base checks
        checkNotNullOrEmpty(source, "module artifact source is missing");
        checkNotNullOrEmpty(sourcePath, "module artifact source path is missing");
        checkNotNullOrEmpty(targetPath, "module artifact target path is missing");
        checkNotNullOrEmpty(environments, "module artifact has no target environments defined");

        // ensure artifact paths are not absolute
        var parsedSourcePath = Path.of(sourcePath);
        if (parsedSourcePath.isAbsolute()) {
          throw new InvalidModuleMetadataException("module artifact source path '" + sourcePath + "' must be relative");
        }
        var parsedTargetPath = Path.of(targetPath);
        if (parsedTargetPath.isAbsolute()) {
          throw new InvalidModuleMetadataException("module artifact target path '" + targetPath + "' must be relative");
        }

        return new DefaultModuleArtifact(source, sourcePath, targetPath, environments);
      })
      .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Parses the declared module dependencies in the given module metadata document.
   *
   * @param metadataJson the metadata document to parse the dependencies from.
   * @return the parsed module dependencies from the given metadata document.
   * @throws NullPointerException           if the given metadata document is null.
   * @throws InvalidModuleMetadataException if an invalid value is encountered while parsing.
   */
  @NonNull
  @Unmodifiable
  private static Collection<ModuleDependency> parseModuleDependencies(@NonNull Document metadataJson) {
    var moduleDependencyDocuments = metadataJson.readObject("dependencies", LIST_DOCUMENT_TYPE, List.<Document>of());
    return moduleDependencyDocuments.stream()
      .map(moduleDependencyDocument -> {
        var moduleId = extractValidString(moduleDependencyDocument, "id", ID_PATTERN);
        var versionRange = moduleDependencyDocument.getString("versionRange", "");
        var dependencyType = moduleDependencyDocument.readObject(
          "dependencyType",
          ModuleDependency.DependencyType.class,
          ModuleDependency.DependencyType.REQUIRED);

        // base checks
        checkNotNullOrEmpty(versionRange, "module dependency range is missing");
        checkNotNullOrEmpty(dependencyType, "module dependency type must be given");

        return new ModuleDependency(moduleId, versionRange, dependencyType);
      })
      .toList();
  }

  /**
   * Parses the declared module external dependencies in the given module metadata document.
   *
   * @param metadataJson the metadata document to parse the external dependencies from.
   * @return the parsed module external dependencies from the given metadata document.
   * @throws NullPointerException           if the given metadata document is null.
   * @throws InvalidModuleMetadataException if an invalid value is encountered while parsing.
   */
  @NonNull
  @Unmodifiable
  private static Collection<ModuleExternalDependency> parseExternalDependencies(@NonNull Document metadataJson) {
    var externalDependencyDocuments = metadataJson.readObject(
      "externalDependencies",
      LIST_DOCUMENT_TYPE,
      List.<Document>of());
    return externalDependencyDocuments.stream()
      .map(externalDependencyDocument -> {
        var loader = externalDependencyDocument.getString("loader", "");
        var optional = externalDependencyDocument.getBoolean("optional", false);
        var environments = externalDependencyDocument.readObject("environments", LIST_STRING_TYPE, List.<String>of());
        var properties = externalDependencyDocument.readDocument("properties", Document.emptyDocument());

        // base checks
        checkNotNullOrEmpty(loader, "external dependency loader is missing");
        checkNotNullOrEmpty(environments, "external dependency environments is missing");

        return new DefaultModuleExternalDependency(loader, environments, properties, optional);
      })
      .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Parses the declared contributors in the given module metadata document.
   *
   * @param metadataJson the metadata document to parse the contributors from.
   * @param key          the key within the document that declares the contributors to parse.
   * @return the parsed module contributors from the given metadata document.
   * @throws NullPointerException           if the given metadata document is null.
   * @throws InvalidModuleMetadataException if an invalid value is encountered while parsing.
   */
  public static Collection<ModuleContributor> parseContributors(@NonNull Document metadataJson, @NonNull String key) {
    var contributors = metadataJson.readObject(key, LIST_DOCUMENT_TYPE, List.<Document>of());
    return contributors.stream()
      .map(contributorDocument -> {
        var name = contributorDocument.getString("name", "");
        var properties = contributorDocument.readDocument("properties", Document.emptyDocument());
        checkNotNullOrEmpty(name, "module-" + key + " name is missing");
        return new DefaultModuleContributor(name, properties);
      })
      .collect(Collectors.toUnmodifiableList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ModuleMetadata parseModuleMetadata(@NonNull JarFile moduleJarFile) throws Exception {
    var metadataJsonEntry = moduleJarFile.getEntry("META-INF/cloudnet-module.json");
    if (metadataJsonEntry == null) {
      return null;
    }

    try (var netadataJsonInputStream = moduleJarFile.getInputStream(metadataJsonEntry)) {
      var metadataJson = this.jsonDocumentFactory.parse(netadataJsonInputStream);
      var schemaVersion = metadataJson.getString("schema");
      if (schemaVersion == null || !schemaVersion.equals(SCHEMA_V1)) {
        // not a schema we can parse here
        return null;
      }

      // extract & validate required module data
      var moduleId = extractValidString(metadataJson, "id", ID_PATTERN);
      var moduleName = extractValidString(metadataJson, "name", NAME_PATTERN);
      var moduleDescription = extractValidString(metadataJson, "description", DESCRIPTION_PATTERN);
      var entrypoint = extractValidString(metadataJson, "entrypoint", ENTRYPOINT_PATTERN);

      // extract the module version
      var version = metadataJson.getString("version", "");
      var semverVersion = SemVerModuleVersion.parseSemVer(version);
      var moduleVersion = Objects.requireNonNullElseGet(semverVersion, () -> UnknownModuleVersion.parse(version));

      // extract other data from the metadata file
      var licenses = metadataJson.readObject("licenses", LIST_STRING_TYPE, List.<String>of());
      var artifacts = parseModuleArtifacts(metadataJson);
      var moduleDependencies = parseModuleDependencies(metadataJson);
      var externalDependencies = parseExternalDependencies(metadataJson);
      var authors = parseContributors(metadataJson, "authors");
      var contributors = parseContributors(metadataJson, "contributors");
      var metaProperties = metadataJson.readDocument("properties", Document.emptyDocument());

      return new DefaultModuleMetadataV1(
        moduleId,
        moduleName,
        moduleDescription,
        entrypoint,
        moduleVersion,
        licenses,
        artifacts,
        moduleDependencies,
        externalDependencies,
        authors,
        contributors,
        metaProperties);
    }
  }
}
