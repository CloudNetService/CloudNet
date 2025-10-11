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

package eu.cloudnetservice.modules.docker.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DockerImageTest {

  @Test
  void testDockerImageRequiredRepository() {
    Assertions.assertThrows(NullPointerException.class, () -> DockerImage.builder().build());
    Assertions.assertDoesNotThrow(() -> DockerImage.builder().repository("test").build());
  }

  @Test
  void testDockerImageNameWithoutTag() {
    var imageNullTag = DockerImage.builder().repository("test").build();
    Assertions.assertEquals("test:latest", imageNullTag.imageName());

    var imageEmptyTag = DockerImage.builder().repository("test").tag("").build();
    Assertions.assertEquals("test:latest", imageEmptyTag.imageName());
  }

  @Test
  void testDockerImageWithNamedTag() {
    var image = DockerImage.builder().repository("test").tag("1.2.3").build();
    Assertions.assertEquals("test:1.2.3", image.imageName());
  }

  @Test
  void testDockerImageWithShaInsteadOfTag() {
    var image = DockerImage.builder()
      .repository("test")
      .tag("sha256:42386c60700180ff9df762c8e091417b294336bb3e579413ff0ed34a14c2d4d5")
      .build();
    Assertions.assertEquals(
      "test@sha256:42386c60700180ff9df762c8e091417b294336bb3e579413ff0ed34a14c2d4d5",
      image.imageName());
  }

  @Test
  void testDockerImageWithRegistry() {
    var image = DockerImage.builder().registry("quay.io").repository("test").build();
    Assertions.assertEquals("quay.io/test:latest", image.imageName());
  }

  @Test
  void testDockerImageWithEverythingSet() {
    var imageWithTagName = DockerImage.builder()
      .registry("quay.io")
      .repository("immobrain/zulu-openjdk-noble")
      .tag("23")
      .build();
    Assertions.assertEquals("quay.io/immobrain/zulu-openjdk-noble:23", imageWithTagName.imageName());

    var imageWithSha = DockerImage.builder()
      .registry("quay.io")
      .repository("immobrain/zulu-openjdk-noble")
      .tag("sha256:42386c60700180ff9df762c8e091417b294336bb3e579413ff0ed34a14c2d4d5")
      .build();
    Assertions.assertEquals(
      "quay.io/immobrain/zulu-openjdk-noble@sha256:42386c60700180ff9df762c8e091417b294336bb3e579413ff0ed34a14c2d4d5",
      imageWithSha.imageName());
  }
}
