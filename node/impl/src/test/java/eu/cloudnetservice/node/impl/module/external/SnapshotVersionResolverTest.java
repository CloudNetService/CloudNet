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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SnapshotVersionResolverTest {

  @Test
  void testResolveVersionWithoutClassifier() throws IOException {
    // language=xml
    var manifest = """
      <metadata modelVersion="1.1.0">
        <groupId>io.netty</groupId>
        <artifactId>netty5-transport-native-io_uring</artifactId>
        <versioning>
          <lastUpdated>20251108135014</lastUpdated>
          <snapshot>
            <timestamp>20251108.135014</timestamp>
            <buildNumber>2</buildNumber>
          </snapshot>
          <snapshotVersions>
            <snapshotVersion>
              <classifier>test-sources</classifier>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.1-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.2-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <extension>pom</extension>
              <value>5.0.0.Alpha6-20251108.3-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>javadoc</classifier>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.4-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>linux-x86_64</classifier>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.5-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>sources</classifier>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.6-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>tests</classifier>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.7-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
          </snapshotVersions>
        </versioning>
        <version>5.0.0.Alpha6-SNAPSHOT</version>
      </metadata>
      """;

    var resolver = new MavenSnapshotVersionResolver();
    var metadataInputStream = new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8));
    var version = resolver.resolveLatestSnapshotVersion(metadataInputStream, null);
    Assertions.assertEquals("5.0.0.Alpha6-20251108.2-2", version);

    metadataInputStream.reset();
    version = resolver.resolveLatestSnapshotVersion(metadataInputStream, "linux-x86_64");
    Assertions.assertEquals("5.0.0.Alpha6-20251108.5-2", version);

    metadataInputStream.reset();
    version = resolver.resolveLatestSnapshotVersion(metadataInputStream, "tests");
    Assertions.assertEquals("5.0.0.Alpha6-20251108.7-2", version);

    metadataInputStream.reset();
    version = resolver.resolveLatestSnapshotVersion(metadataInputStream, "simrail");
    Assertions.assertNull(version);
  }

  @Test
  void testFailsGracefullyOnInvalidXml() throws IOException {
    var manifest = """
      <metadata modelVersion="1.1.0">
        <groupId>io.netty</groupId>
        <artifactId>netty5-transport-native-io_uring</artifactId>
        <versioning>
          <lastUpdated>20251108135014</lastUpdated>
          <snapshot>
            <timestamp>20251108.135014</timestamp>
            <buildNumber>2</buildNumber>
          </snapshot>
          <snapshotVersions>
            <snapshotVersion>
              <classifier>test-sources</classifier>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.1-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.2-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <extension>pom</extension>
              <value>5.0.0.Alpha6-20251108.3-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>javadoc</classifier>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.4-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>linux-x86_64</classifier>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.5-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>sources</classifier>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.6-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
            <snapshotVersion>
              <classifier>tests</classifier>
              <extension>jar</extension>
              <value>5.0.0.Alpha6-20251108.7-2</value>
              <updated>20251108135014</updated>
            </snapshotVersion>
          </snapshotVersions>
        <version>5.0.0.Alpha6-SNAPSHOT</version>
      </metadata>
      """;

    var resolver = new MavenSnapshotVersionResolver();
    var metadataInputStream = new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8));
    var version = resolver.resolveLatestSnapshotVersion(metadataInputStream, null);
    Assertions.assertNull(version);
  }

  @Test
  void testFailsGracefullyIfVersionPartIsMissing() throws IOException {
    // language=xml
    var manifest = """
      <metadata modelVersion="1.1.0">
        <groupId>io.netty</groupId>
        <artifactId>netty5-transport-native-io_uring</artifactId>
        <versioning>
          <lastUpdated>20251108135014</lastUpdated>
          <snapshot>
            <timestamp>20251108.135014</timestamp>
            <buildNumber>2</buildNumber>
          </snapshot>
        </versioning>
        <version>5.0.0.Alpha6-SNAPSHOT</version>
      </metadata>
      """;

    var resolver = new MavenSnapshotVersionResolver();
    var metadataInputStream = new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8));
    var version = resolver.resolveLatestSnapshotVersion(metadataInputStream, null);
    Assertions.assertNull(version);
  }
}
