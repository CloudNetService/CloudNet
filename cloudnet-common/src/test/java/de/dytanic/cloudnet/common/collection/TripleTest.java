package de.dytanic.cloudnet.common.collection;

import org.junit.Assert;
import org.junit.Test;

public final class TripleTest {

  @Test
  public void testTriple() {
    Triple<String, Integer, Boolean> pair = new Triple<>();

    Assert.assertNull(pair.getFirst());
    Assert.assertNull(pair.getSecond());

    pair.setFirst("Test");
    pair.setSecond(5);
    pair.setThird(true);

    Assert.assertNotNull(pair.getFirst());
    Assert.assertNotNull(pair.getSecond());
    Assert.assertNotNull(pair.getThird());

    Assert.assertEquals("Test", pair.getFirst());
    Assert.assertEquals(5, pair.getSecond().intValue());
    Assert.assertTrue(pair.getThird());

    pair = new Triple<>("foobar", 44, false);

    Assert.assertNotNull(pair.getFirst());
    Assert.assertNotNull(pair.getSecond());
    Assert.assertNotNull(pair.getThird());

    Assert.assertEquals("foobar", pair.getFirst());
    Assert.assertEquals(44, pair.getSecond().intValue());
    Assert.assertFalse(pair.getThird());
  }
}