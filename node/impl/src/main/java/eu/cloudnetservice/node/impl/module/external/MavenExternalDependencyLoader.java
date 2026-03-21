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

package eu.cloudnetservice.node.impl.module.external;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import eu.cloudnetservice.driver.document.property.DocProperty;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.node.module.dependency.ModuleExternalDependency;
import eu.cloudnetservice.node.module.dependency.ModuleExternalDependencyLoader;
import eu.cloudnetservice.utils.base.StringUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Map;
import kong.unirest.core.RawResponse;
import kong.unirest.core.Unirest;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader implementation for dependencies provided by a remote maven repository.
 *
 * @since 4.0
 */
@Singleton
@AutoService(services = ModuleExternalDependencyLoader.class, name = "maven")
public final class MavenExternalDependencyLoader implements ModuleExternalDependencyLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenExternalDependencyLoader.class);

  /**
   * Format for the url to the version-level maven manifest file. Consists of the following parts (in order) that must
   * be filled in: the repo base url, the group id (separated by slashes), the artifact id, the version to get the
   * metadata information of.
   */
  private static final String VERSION_METADATA_URL_FORMAT = "%s%s/%s/%s/maven-metadata.xml";
  /**
   * Format for the url to download a single file from a remote maven repository. Consists of the following parts (in
   * order) that must be filled in: the repo base url, the group id (separated by slashes), the artifact id, the
   * version, the name of the file to download.
   */
  private static final String VERSION_JAR_URL_FORMAT = "%s%s/%s/%s/%s";

  private static final DocProperty<String> PROP_GROUP_ID = validMavenIdentifierStringProp("groupId");
  private static final DocProperty<String> PROP_ARTIFACT_ID = validMavenIdentifierStringProp("artifactId");
  private static final DocProperty<String> PROP_VERSION = validMavenIdentifierStringProp("version");
  private static final DocProperty<String> PROP_CLASSIFIER = validMavenIdentifierStringProp("classifier");

  /**
   * Property for the remote repository url to download from. The returned uri string is validated to use {@code http}
   * or {@code https} as it's scheme as always ends with a slash.
   */
  private static final DocProperty<String> PROP_REPO_URI =
    DocProperty.property("repoUrl", String.class).withReadRewrite(val -> {
      try {
        var uri = new URI(val).normalize();
        var uriScheme = StringUtil.toLower(uri.getScheme());
        Preconditions.checkArgument(uri.getHost() != null, "maven repo url '%s' has no host part", val);
        Preconditions.checkArgument(
          "http".equals(uriScheme) || "https".equals(uriScheme),
          "maven repo url '%s' uses invalid scheme", val);
        var uriAsString = uri.toString();
        return uriAsString.endsWith("/") ? uriAsString : uriAsString.concat("/");
      } catch (URISyntaxException exception) {
        var msg = String.format(
          "maven repo url '%s' is invalid @%d: %s",
          val, exception.getIndex(), exception.getReason());
        throw new IllegalArgumentException(msg);
      }
    });

  /**
   * Property for the checksum to validate a downloaded artifact against. This property is optional and ignored if the
   * version of the artifact is a snapshot. In case of a snapshot a checksum is retrieved from the remote repository
   * after the latest snapshot version was resolved. The map entry returned by this property contains the checksum
   * algorithm as the key and the expected checksum as the value.
   */
  private static final DocProperty<Map.Entry<String, String>> PROP_CHECKSUM =
    DocProperty.property("checksum", String.class).withReadRewrite(val -> {
      var parts = val.split(":");
      Preconditions.checkArgument(parts.length == 2, "dependency checksum must have format <type>:<checksum>");
      return Map.entry(parts[0], parts[1]);
    });

  private final FileChecksumValidator fileChecksumValidator;
  private final MavenSnapshotVersionResolver snapshotVersionResolver;

  @Inject
  MavenExternalDependencyLoader(@NonNull MavenSnapshotVersionResolver snapshotVersionResolver) {
    this.snapshotVersionResolver = snapshotVersionResolver;
    this.fileChecksumValidator = new FileChecksumValidator(HexFormat.of());
  }

  /**
   * Constructs a new string property that normalizes blank values to null and ensures that no potentially unsafe
   * special characters are contained in it.
   *
   * @param key the key of the property.
   * @return a new validated string document property.
   * @throws NullPointerException if the given key is null.
   */
  private static @NonNull DocProperty<String> validMavenIdentifierStringProp(@NonNull String key) {
    return DocProperty.property(key, String.class)
      .withReadRewrite(val -> val.isBlank() ? null : val)
      .withReadRewrite(val -> {
        if (val != null && (val.contains("..") || val.contains("/") || val.contains("\\") || val.contains(":"))) {
          var msg = String.format("maven dependency property %s (value '%s') contains invalid char", key, val);
          throw new IllegalArgumentException(msg);
        }

        return val;
      });
  }

  /**
   * Constructs a new exception using a common message to inform that the given property has no value defined.
   *
   * @param property the property whose value is missing.
   * @return a new exception to inform that the given property has no value defined.
   * @throws NullPointerException if the given property is null.
   */
  private static @NonNull RuntimeException constructMissingPropException(@NonNull DocProperty<?> property) {
    return new IllegalArgumentException("missing required property for maven dependency: " + property.key());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return "maven";
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Path loadExternalDependency(@NonNull Path cachePath, @NonNull ModuleExternalDependency dep) throws Exception {
    var repoUri = dep.readPropertyOrThrow(PROP_REPO_URI, () -> constructMissingPropException(PROP_REPO_URI));
    var groupId = dep.readPropertyOrThrow(PROP_GROUP_ID, () -> constructMissingPropException(PROP_GROUP_ID));
    var artifactId = dep.readPropertyOrThrow(PROP_ARTIFACT_ID, () -> constructMissingPropException(PROP_ARTIFACT_ID));
    var version = dep.readPropertyOrThrow(PROP_VERSION, () -> constructMissingPropException(PROP_VERSION));
    var classifier = dep.readProperty(PROP_CLASSIFIER);
    var checksum = dep.readProperty(PROP_CHECKSUM);

    // in case a snapshot version is given, we need to resolve it to the actual timestamped version first
    var originalVersion = version;
    var isSnapshot = originalVersion.endsWith("-SNAPSHOT");
    if (isSnapshot) {
      var metadataUrl = String.format(
        VERSION_METADATA_URL_FORMAT,
        repoUri, groupId.replace('.', '/'), artifactId, version);
      var snapshotVersionFetchResponse = Unirest.get(metadataUrl)
        .requestTimeout(5000)
        .accept("application/xml")
        .asObject(response -> {
          Preconditions.checkState(
            response.getStatus() == 200,
            "snapshot version resolve of %s:%s failed, metadata fetch returned status %s",
            groupId, artifactId, response.getStatus());

          try (var responseContentStream = response.getContent()) {
            return Verify.verifyNotNull(
              this.snapshotVersionResolver.resolveLatestSnapshotVersion(responseContentStream, classifier),
              "snapshot version resolve of %s:%s (classifier %s) failed, metadata did not contain a version",
              groupId, artifactId, classifier);
          } catch (IOException exception) {
            var msg = String.format(
              "snapshot version resolve of %s:%s failed, failed to read metadata response: %s",
              groupId, artifactId, exception.getMessage());
            throw new IllegalStateException(msg, exception);
          }
        });
      checksum = null; // we resolved some latest version, most likely the checksum is invalid
      version = snapshotVersionFetchResponse.getBody();
      LOGGER.debug("Resolved timestamped snapshot version of {}:{} to {}", groupId, artifactId, version);
    }

    // check if we need to download the version, or if the version is already available
    var fileName = artifactId
      .concat("-")
      .concat(version)
      .concat(classifier == null ? "" : "-".concat(classifier))
      .concat(".jar");
    var fileCachePath = cachePath
      .resolve(groupId.replace('.', '/'))
      .resolve(artifactId)
      .resolve(originalVersion)
      .resolve(fileName);
    var isAvailableLocally = Files.exists(fileCachePath)
      && (checksum == null || this.fileChecksumValidator.validateFileChecksum(checksum, fileCachePath));
    if (isAvailableLocally) {
      LOGGER.debug("Skipping download of dependency {}:{}:{}, file is available locally", groupId, artifactId, version);
      return fileCachePath;
    }

    // download the dependency jar file from the remote repository and put it at the expected cache path
    LOGGER.debug(
      "Dependency {}:{}:{} does not exist at expected path {}, trying to download it from {}",
      groupId, artifactId, version, fileCachePath, repoUri);
    var fileUrl = String.format(
      VERSION_JAR_URL_FORMAT,
      repoUri, groupId.replace('.', '/'), artifactId, version, fileName);
    var response = Unirest.get(fileUrl).asObject(RawResponse::getContent);
    Preconditions.checkState(
      response.getStatus() == 200,
      "failed to download version %s of %s:%s from %s, server returned status %s",
      version, groupId, artifactId, repoUri, response.getStatus());
    try (var fileContentStream = response.getBody()) {
      FileUtil.copy(fileContentStream, fileCachePath);
    }

    // verify the checksum of the file, if provided
    if (checksum != null) {
      LOGGER.debug("Verifying downloaded dependency {}:{} matches expected checksum {}", groupId, artifactId, checksum);
      var downloadIsValid = this.fileChecksumValidator.validateFileChecksum(checksum, fileCachePath);
      if (!downloadIsValid) {
        FileUtil.delete(fileCachePath);
        var msg = String.format(
          "Downloaded dependency file of %s:%s from %s did not match expected checksum %s",
          groupId, artifactId, repoUri, checksum);
        throw new IllegalStateException(msg);
      }
    }

    return fileCachePath;
  }
}
