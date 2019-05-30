package de.dytanic.cloudnet.common.collection;

import java.util.Arrays;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Test;

public final class MapsTest {

  @Test
  public void testNotNull() {
    Assert.assertNotNull(Maps.newConcurrentHashMap());
    Assert.assertNotNull(Maps.newHashMap());
    Assert.assertNotNull(Maps.newLinkedHashMap());
    Assert.assertNotNull(Maps.newConcurrentSkipListMap());
    Assert.assertNotNull(Maps.newIdentityHashMap());
  }

  @Test
  public void testMisc() {
    Assert.assertEquals(3,
      Maps.newMapByValues(Arrays.asList("test", "test1", "test_3"),
        new Function<String, Integer>() {
          @Override
          public Integer apply(String v) {
            return v.length();
          }
        }).size());

    Assert.assertEquals(3, Maps.newMapByKeys(Arrays.asList(4, 3, 2),
      new Function<Integer, String>() {
        @Override
        public String apply(Integer v) {
          return v.toString();
        }
      }).size());

    Assert.assertEquals(3,
      Maps.of(new Pair<>("foo", "bar"), new Pair<>("hello", "world"),
        new Pair<>("nico", "sascha")).size());
  }
}