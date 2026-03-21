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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Resolver for the latest snapshot version defined in a version-level metadata file of a remote maven repository. The
 * returned versions always correspond to a jar file, never to something else pushed to the repository (for example a
 * pom file).
 *
 * @since 4.0
 */
@Singleton
final class MavenSnapshotVersionResolver {

  /**
   * XPath expression to get each snapshot version entry in a version-level metadata file.
   */
  private static final String XPATH_SNAPSHOT_VERSION = "/metadata/versioning/snapshotVersions/snapshotVersion";

  private final XPathFactory xPathFactory;
  private final DocumentBuilderFactory documentBuilderFactory;

  @Inject
  public MavenSnapshotVersionResolver() {
    this.xPathFactory = XPathFactory.newInstance();
    this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
    this.documentBuilderFactory.setValidating(false);
    this.documentBuilderFactory.setIgnoringComments(true);
    this.documentBuilderFactory.setIgnoringElementContentWhitespace(true);
  }

  /**
   * Checks if the given actual classifier is equal to the given expected one.
   *
   * @param expected the expected classifier, {@code null} if no classifier is requested.
   * @param actual   the actual classifier, can be an empty string if not set.
   * @return true if the given actual classifier is equal to the expected one, false otherwise.
   * @throws NullPointerException if the given actual classifier is null.
   */
  private static boolean isSameClassifier(@Nullable String expected, @NonNull String actual) {
    // xpath evaluation will result in 'actual' being an empty string if not set.
    // expected=null just means that we don't expect a classifier, not that we want *any* classifier
    return (expected == null && actual.isEmpty()) || (expected != null && expected.equals(actual));
  }

  /**
   * Resolves the latest snapshot version from the given version-level manifest file. An optional classifier can be
   * specified to resolve the version of the artifact with the given classifier. The returned version always corresponds
   * to the jar file version.
   *
   * @param versionMetadata    the stream holding the version-level manifest file to resolve from.
   * @param expectedClassifier the classifier that the version to resolve must have.
   * @return thg resolved version from the given manifest, {@code null} if no matching version was found.
   * @throws IOException          if an i/o error occurs while reading the given stream.
   * @throws NullPointerException if the given version metadata input stream is null.
   */
  public @Nullable String resolveLatestSnapshotVersion(
    @NonNull InputStream versionMetadata,
    @Nullable String expectedClassifier
  ) throws IOException {
    try {
      var xpath = this.xPathFactory.newXPath();
      var documentBuilder = this.documentBuilderFactory.newDocumentBuilder();
      var versionMetadataDocument = documentBuilder.parse(versionMetadata);
      var versions = (NodeList) xpath.evaluate(XPATH_SNAPSHOT_VERSION, versionMetadataDocument, XPathConstants.NODESET);
      for (var index = 0; index < versions.getLength(); index++) {
        var item = versions.item(index);
        var version = (String) xpath.evaluate("value", item, XPathConstants.STRING);
        var extension = (String) xpath.evaluate("extension", item, XPathConstants.STRING);
        var classifier = (String) xpath.evaluate("classifier", item, XPathConstants.STRING);
        if (extension.equals("jar") && isSameClassifier(expectedClassifier, classifier)) {
          return version;
        }
      }

      return null;
    } catch (ParserConfigurationException exception) {
      throw new AssertionError("misconfigured document parser, should not happen", exception);
    } catch (SAXException | XPathExpressionException exception) {
      // invalid metadata file, or it doesn't contain any snapshot version info
      return null;
    }
  }
}
