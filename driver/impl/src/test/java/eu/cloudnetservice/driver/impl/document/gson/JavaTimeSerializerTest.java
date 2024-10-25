/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.impl.document.gson;

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@EnableServicesInject
public class JavaTimeSerializerTest {

  static Stream<Arguments> timeClassProvider() {
    return Stream.of(
      // Instant
      Arguments.of(Instant.MIN),
      Arguments.of(Instant.MAX),
      Arguments.of(Instant.EPOCH),
      Arguments.of(Instant.now()),
      Arguments.of(Instant.now().minusSeconds(500)),
      // Duration
      Arguments.of(Duration.ZERO),
      Arguments.of(Duration.ofDays(5)),
      Arguments.of(Duration.ofMinutes(50000)),
      Arguments.of(Duration.ofHours(999999999)),
      Arguments.of(Duration.ofSeconds(120000)),
      // LocalDate
      Arguments.of(LocalDate.EPOCH),
      Arguments.of(LocalDate.MIN),
      Arguments.of(LocalDate.MAX),
      Arguments.of(LocalDate.now()),
      Arguments.of(LocalDate.of(2003, 9, 18)),
      Arguments.of(LocalDate.of(1999, 12, 31)),
      Arguments.of(LocalDate.of(1989, 11, 9)),
      Arguments.of(LocalDate.of(1871, 1, 18)),
      // LocalTime
      Arguments.of(LocalTime.MIN),
      Arguments.of(LocalTime.MAX),
      Arguments.of(LocalTime.MIDNIGHT),
      Arguments.of(LocalTime.NOON),
      Arguments.of(LocalTime.now()),
      Arguments.of(LocalTime.of(11, 11)),
      Arguments.of(LocalTime.of(16, 12, 34, 667)),
      // OffsetTime
      Arguments.of(OffsetTime.MIN),
      Arguments.of(OffsetTime.MAX),
      Arguments.of(OffsetTime.now()),
      Arguments.of(OffsetTime.of(LocalTime.now(), ZoneOffset.UTC)),
      Arguments.of(OffsetTime.of(LocalTime.now(), ZoneOffset.ofHours(5))),
      Arguments.of(OffsetTime.of(LocalTime.now(), ZoneOffset.ofHours(-3))),
      // LocalDateTime
      Arguments.of(LocalDateTime.MIN),
      Arguments.of(LocalDateTime.MAX),
      Arguments.of(LocalDateTime.now()),
      Arguments.of(LocalDateTime.of(1945, 8, 6, 8, 16, 2)),
      Arguments.of(LocalDateTime.of(1945, 8, 9, 11, 2)),
      // OffsetDateTime
      Arguments.of(OffsetDateTime.MIN),
      Arguments.of(OffsetDateTime.MAX),
      Arguments.of(OffsetDateTime.now()),
      Arguments.of(OffsetDateTime.of(LocalDateTime.of(2058, 8, 15, 23, 12), ZoneOffset.ofHours(-5))),
      Arguments.of(OffsetDateTime.of(LocalDateTime.of(1986, 4, 26, 1, 23, 44), ZoneOffset.ofHours(3)))
    );
  }

  @ParameterizedTest
  @MethodSource("timeClassProvider")
  public void testTimeClassSerializationWithJson(Object timeInstance) {
    var document = DocumentFactory.json().newDocument("time", timeInstance);
    var readTimeInstance = document.readObject("time", timeInstance.getClass());

    Assertions.assertNotNull(readTimeInstance);
    Assertions.assertEquals(timeInstance, readTimeInstance);
  }
}
