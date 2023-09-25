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

package eu.cloudnetservice.driver.network.rpc.object;

import com.google.common.collect.Maps;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.TestInjectionLayerConfigurator;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.defaults.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.service.ProcessSnapshot;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceId;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ThreadSnapshot;
import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DefaultObjectMapperTest {

  static Stream<Arguments> nonNestedTypesProvider() {
    return Stream.of(
      Arguments.of(null, String.class),
      Arguments.of(StringUtil.generateRandomString(25), null),
      Arguments.of(ThreadLocalRandom.current().nextInt(), null),
      Arguments.of(ThreadLocalRandom.current().nextLong(), null),
      Arguments.of(ThreadLocalRandom.current().nextFloat(), null),
      Arguments.of(ThreadLocalRandom.current().nextDouble(), null),
      Arguments.of(ThreadLocalRandom.current().nextBoolean(), null),
      Arguments.of(StringUtil.generateRandomString(1).charAt(0), null),
      Arguments.of((byte) ThreadLocalRandom.current().nextInt(Byte.MAX_VALUE), null),
      Arguments.of((short) ThreadLocalRandom.current().nextInt(Short.MAX_VALUE), null));
  }

  static Stream<Arguments> enumDataProvider() {
    return Stream.of(
      Arguments.of(ServiceLifeCycle.PREPARED),
      Arguments.of(ServiceLifeCycle.STOPPED),
      Arguments.of(ServiceLifeCycle.DELETED));
  }

  static Stream<Arguments> arrayDataProvider() {
    return Stream.of(
      Arguments.of((Object) new String[]{"test1", "test2", "test3"}),
      Arguments.of((Object) new Integer[]{1234, 5678, 9012}),
      Arguments.of((Object) new ServiceLifeCycle[][]{ServiceLifeCycle.values(), ServiceLifeCycle.values()}));
  }

  static Stream<Arguments> listDataProvider() {
    return Stream.of(
      Arguments.of(Arrays.asList("test", "test1", "test2"), String.class),
      Arguments.of(Arrays.asList(1234, 5678, 9012, 3456, 7890), Integer.class),
      Arguments.of(
        Arrays.asList(Collections.singleton("test"), Collections.singleton("test2"), Arrays.asList("test3", "test4")),
        TypeFactory.parameterizedClass(List.class, String.class)));
  }

  static Stream<Arguments> mapDataProvider() {
    return Stream.of(
      Arguments.of(Map.of("test", "test1", "test2", "test3"), String.class, String.class),
      Arguments.of(Map.of("test", 123, "test2", 456), String.class, Integer.class),
      Arguments.of(
        Map.of("test", Arrays.asList(123, 456), "test2", Arrays.asList(678, 456)),
        String.class, TypeFactory.parameterizedClass(List.class, Integer.class)),
      Arguments.of(
        Map.of("test", Map.of("test2", Arrays.asList(1234, 3456))),
        String.class, TypeFactory.parameterizedClass(
          Map.class,
          String.class,
          TypeFactory.parameterizedClass(List.class, Integer.class))));
  }

  static Stream<Arguments> optionalDataProvider() {
    return Stream.of(
      Arguments.of(Optional.of("test"), String.class),
      Arguments.of(Optional.of(1234), Integer.class),
      Arguments.of(
        Optional.of(Arrays.asList("test", "test1")),
        TypeFactory.parameterizedClass(List.class, String.class)),
      Arguments.of(
        Optional.of(Map.of("test", "test1", "test2", "test3")),
        TypeFactory.parameterizedClass(Map.class, String.class, String.class)));
  }

  static Stream<Arguments> dataClassProvider() {
    return Stream.of(
      Arguments.of(new AllPrimitiveTypesDataClass()),
      Arguments.of(ThreadSnapshot.from(Thread.currentThread())),
      Arguments.of(ServiceId.builder()
        .taskName("Lobby")
        .nameSplitter("hello")
        .taskServiceId(156)
        .allowedNodes(Set.of("Node-245"))
        .environment(ServiceEnvironmentType.WATERDOG_PE)
        .build()),
      Arguments.of(new ServiceInfoSnapshot(
        System.currentTimeMillis(),
        new HostAndPort("127.0.1.1", 99),
        ProcessSnapshot.self(),
        ServiceConfiguration.builder()
          .taskName("Lobby")
          .environment(ServiceEnvironmentType.BUNGEECORD)
          .maxHeapMemory(512)
          .startPort(1234)
          .build(),
        System.nanoTime(),
        ServiceLifeCycle.STOPPED,
        Document.newJsonDocument().append("test", 1234)))
    );
  }

  static Stream<Arguments> timeClassProvider() {
    return Stream.of(
      // Year
      Arguments.of(Year.MIN_VALUE),
      Arguments.of(Year.MAX_VALUE),
      Arguments.of(Year.of(1902)),
      Arguments.of(Year.of(-57889)),
      Arguments.of(Year.now()),
      // Period
      Arguments.of(Period.ZERO),
      Arguments.of(Period.ofDays(15)),
      Arguments.of(Period.ofWeeks(178)),
      Arguments.of(Period.ofYears(199943)),
      Arguments.of(Period.of(12234, 9990, 22345)),
      // ZoneId
      Arguments.of(ZoneOffset.UTC),
      Arguments.of(ZoneOffset.MIN),
      Arguments.of(ZoneOffset.MAX),
      Arguments.of(ZoneOffset.ofHours(5)),
      Arguments.of(ZoneOffset.ofHours(-3)),
      Arguments.of(ZoneId.of("Europe/Berlin")),
      Arguments.of(ZoneId.of("America/New_York")),
      Arguments.of(ZoneId.of("GMT+5")),
      Arguments.of(ZoneId.of("UTC-2")),
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
      // MonthDay
      Arguments.of(MonthDay.of(12, 5)),
      Arguments.of(MonthDay.of(9, 18)),
      Arguments.of(MonthDay.of(1, 1)),
      Arguments.of(MonthDay.of(6, 30)),
      Arguments.of(MonthDay.of(3, 1)),
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
      // YearMonth
      Arguments.of(YearMonth.now()),
      Arguments.of(YearMonth.of(1961, 8)),
      Arguments.of(YearMonth.of(1929, 10)),
      Arguments.of(YearMonth.of(2024, 11)),
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
      // ZonedDateTime
      Arguments.of(ZonedDateTime.now()),
      Arguments.of(ZonedDateTime.of(LocalDateTime.of(2034, 6, 6, 12, 5), ZoneId.of("UTC-2"))),
      Arguments.of(ZonedDateTime.of(LocalDateTime.of(2011, 3, 11, 14, 46, 23), ZoneId.of("GMT+9"))),
      // OffsetDateTime
      Arguments.of(OffsetDateTime.MIN),
      Arguments.of(OffsetDateTime.MAX),
      Arguments.of(OffsetDateTime.now()),
      Arguments.of(OffsetDateTime.of(LocalDateTime.of(2058, 8, 15, 23, 12), ZoneOffset.ofHours(-5))),
      Arguments.of(OffsetDateTime.of(LocalDateTime.of(1986, 4, 26, 1, 23, 44), ZoneOffset.ofHours(3)))
    );
  }

  @BeforeAll
  static void setupBootInjectionLayer() {
    TestInjectionLayerConfigurator.loadAutoconfigureBindings();
  }

  @Order(0)
  @ParameterizedTest
  @MethodSource("nonNestedTypesProvider")
  void testNonNestedTypes(Object o, Type type) {
    var mapper = new DefaultObjectMapper();
    var buf = DataBuf.empty();

    mapper.writeObject(buf, o);
    Assertions.assertEquals(o, mapper.readObject(buf, type == null ? o.getClass() : type));
  }

  @Order(10)
  @ParameterizedTest
  @MethodSource("enumDataProvider")
  void testEnumSerialization(Enum<?> constant) {
    var mapper = new DefaultObjectMapper();
    var buf = DataBuf.empty();

    mapper.writeObject(buf, constant);
    Assertions.assertEquals(constant, mapper.readObject(buf, constant.getClass()));
  }

  @Order(20)
  @ParameterizedTest
  @MethodSource("arrayDataProvider")
  <T> void testArraySerialization(T[] array) {
    var mapper = new DefaultObjectMapper();
    var buf = DataBuf.empty();

    mapper.writeObject(buf, array);
    Assertions.assertArrayEquals(array, mapper.readObject(buf, array.getClass()));
  }

  @Order(30)
  @ParameterizedTest
  @MethodSource("listDataProvider")
  <T> void testListSerialization(List<T> list, Type parameterType) {
    var mapper = new DefaultObjectMapper();
    var buf = DataBuf.empty();

    mapper.writeObject(buf, list);
    List<T> result = mapper.readObject(buf, TypeFactory.parameterizedClass(List.class, parameterType));

    Assertions.assertNotNull(result);
    Assertions.assertIterableEquals(list, result);
  }

  @Order(40)
  @ParameterizedTest
  @MethodSource("mapDataProvider")
  <K, V> void testMapSerialization(Map<K, V> map, Type keyType, Type valueType) {
    var mapper = new DefaultObjectMapper();
    var buf = DataBuf.empty();

    mapper.writeObject(buf, map);
    Map<K, V> result = mapper.readObject(buf, TypeFactory.parameterizedClass(Map.class, keyType, valueType));

    Assertions.assertNotNull(result);
    Assertions.assertTrue(Maps.difference(map, result).areEqual());
  }

  @Order(50)
  @ParameterizedTest
  @MethodSource("optionalDataProvider")
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  <T> void testOptionalSerialization(Optional<T> o, Type parameterType) {
    var mapper = new DefaultObjectMapper();
    var buf = DataBuf.empty();

    mapper.writeObject(buf, o);
    Optional<T> result = mapper.readObject(buf, TypeFactory.parameterizedClass(Optional.class, parameterType));

    Assertions.assertNotNull(result);
    Assertions.assertEquals(o.isPresent(), result.isPresent());
    Assertions.assertEquals(o.orElse(null), result.orElse(null));
  }

  @Order(60)
  @ParameterizedTest
  @MethodSource("dataClassProvider")
  void testDataClassSerialization(Object o) {
    var mapper = new DefaultObjectMapper();
    var buf = DataBuf.empty();

    mapper.writeObject(buf, o);
    var result = mapper.readObject(buf, o.getClass());

    Assertions.assertNotNull(result);
    Assertions.assertEquals(o, result);
  }

  @Test
  @Order(70)
  void testByteArrayWriting() {
    // special case which needs a separate test
    var bytes = new byte[]{0x25, 0x26, 0x0F, 0x3F, 0x4F, 0x64};

    var mapper = new DefaultObjectMapper();
    var buf = DataBuf.empty();

    mapper.writeObject(buf, bytes);
    byte[] result = mapper.readObject(buf, byte[].class);

    Assertions.assertNotNull(result);
    Assertions.assertArrayEquals(bytes, result);
  }

  @Order(80)
  @ParameterizedTest
  @MethodSource("timeClassProvider")
  void testTimeClassSerialization(Object timeInstance) {
    var mapper = new DefaultObjectMapper();
    var buf = DataBuf.empty();

    mapper.writeObject(buf, timeInstance);
    var result = mapper.readObject(buf, timeInstance.getClass());

    Assertions.assertNotNull(result);
    Assertions.assertEquals(timeInstance, result);
  }
}
