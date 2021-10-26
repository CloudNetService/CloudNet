/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.rpc.object;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ThreadSnapshot;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestMethodOrder(OrderAnnotation.class)
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
        getParameterized(List.class, String.class)));
  }

  static Stream<Arguments> mapDataProvider() {
    return Stream.of(
      Arguments.of(ImmutableMap.of("test", "test1", "test2", "test3"), String.class, String.class),
      Arguments.of(ImmutableMap.of("test", 123, "test2", 456), String.class, Integer.class),
      Arguments.of(
        ImmutableMap.of("test", Arrays.asList(123, 456), "test2", Arrays.asList(678, 456)),
        String.class, getParameterized(List.class, Integer.class)),
      Arguments.of(
        ImmutableMap.of("test", ImmutableMap.of("test2", Arrays.asList(1234, 3456))),
        String.class, getParameterized(Map.class, String.class, getParameterized(List.class, Integer.class))));
  }

  static Stream<Arguments> optionalDataProvider() {
    return Stream.of(
      Arguments.of(Optional.of("test"), String.class),
      Arguments.of(Optional.of(1234), Integer.class),
      Arguments.of(Optional.of(Arrays.asList("test", "test1")), getParameterized(List.class, String.class)),
      Arguments.of(
        Optional.of(ImmutableMap.of("test", "test1", "test2", "test3")),
        getParameterized(Map.class, String.class, String.class)));
  }

  static Stream<Arguments> dataClassProvider() {
    return Stream.of(
      Arguments.of(new AllPrimitiveTypesDataClass()),
      Arguments.of(new ThreadSnapshot(Thread.currentThread())),
      Arguments.of(new ServiceId(
        UUID.randomUUID(),
        "Lobby",
        1,
        "Node-1",
        Collections.emptyList(),
        ServiceEnvironmentType.GLOWSTONE)),
      Arguments.of(new ServiceInfoSnapshot(
        System.currentTimeMillis(),
        new HostAndPort("127.0.1.1", 99),
        0,
        ServiceLifeCycle.PREPARED,
        ProcessSnapshot.self(),
        JsonDocument.newDocument("test", 1234),
        ServiceConfiguration.builder()
          .task("Lobby")
          .environment(ServiceEnvironmentType.BUNGEECORD)
          .maxHeapMemory(512)
          .startPort(1234)
          .build()))
    );
  }

  private static Type getParameterized(Type rawType, Type... typeArguments) {
    return TypeToken.getParameterized(rawType, typeArguments).getType();
  }

  @Order(0)
  @ParameterizedTest
  @MethodSource("nonNestedTypesProvider")
  void testNonNestedTypes(Object o, Type type) {
    ObjectMapper mapper = new DefaultObjectMapper();
    DataBuf.Mutable buf = DataBuf.empty();

    mapper.writeObject(buf, o);
    Assertions.assertEquals(o, mapper.readObject(buf, type == null ? o.getClass() : type));
  }

  @Order(10)
  @ParameterizedTest
  @MethodSource("enumDataProvider")
  void testEnumSerialization(Enum<?> constant) {
    ObjectMapper mapper = new DefaultObjectMapper();
    DataBuf.Mutable buf = DataBuf.empty();

    mapper.writeObject(buf, constant);
    Assertions.assertEquals(constant, mapper.readObject(buf, constant.getClass()));
  }

  @Order(20)
  @ParameterizedTest
  @MethodSource("arrayDataProvider")
  <T> void testArraySerialization(T[] array) {
    ObjectMapper mapper = new DefaultObjectMapper();
    DataBuf.Mutable buf = DataBuf.empty();

    mapper.writeObject(buf, array);
    Assertions.assertArrayEquals(array, mapper.readObject(buf, array.getClass()));
  }

  @Order(30)
  @ParameterizedTest
  @MethodSource("listDataProvider")
  <T> void testListSerialization(List<T> list, Type parameterType) {
    ObjectMapper mapper = new DefaultObjectMapper();
    DataBuf.Mutable buf = DataBuf.empty();

    mapper.writeObject(buf, list);
    List<T> result = mapper.readObject(buf, getParameterized(List.class, parameterType));

    Assertions.assertNotNull(result);
    Assertions.assertIterableEquals(list, result);
  }

  @Order(40)
  @ParameterizedTest
  @MethodSource("mapDataProvider")
  <K, V> void testMapSerialization(Map<K, V> map, Type keyType, Type valueType) {
    ObjectMapper mapper = new DefaultObjectMapper();
    DataBuf.Mutable buf = DataBuf.empty();

    mapper.writeObject(buf, map);
    Map<K, V> result = mapper.readObject(buf, getParameterized(Map.class, keyType, valueType));

    Assertions.assertNotNull(result);
    Assertions.assertTrue(Maps.difference(map, result).areEqual());
  }

  @Order(50)
  @ParameterizedTest
  @MethodSource("optionalDataProvider")
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  <T> void testOptionalSerialization(Optional<T> o, Type parameterType) {
    ObjectMapper mapper = new DefaultObjectMapper();
    DataBuf.Mutable buf = DataBuf.empty();

    mapper.writeObject(buf, o);
    Optional<T> result = mapper.readObject(buf, getParameterized(Optional.class, parameterType));

    Assertions.assertNotNull(result);
    Assertions.assertEquals(o.isPresent(), result.isPresent());
    Assertions.assertEquals(o.orElse(null), result.orElse(null));
  }

  @Order(60)
  @ParameterizedTest
  @MethodSource("dataClassProvider")
  void testDataClassSerialization(Object o) {
    ObjectMapper mapper = new DefaultObjectMapper();
    DataBuf.Mutable buf = DataBuf.empty();

    mapper.writeObject(buf, o);
    Object result = mapper.readObject(buf, o.getClass());

    Assertions.assertNotNull(result);
    Assertions.assertEquals(o, result);
  }
}
