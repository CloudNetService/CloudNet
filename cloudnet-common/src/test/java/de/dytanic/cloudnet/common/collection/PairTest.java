package de.dytanic.cloudnet.common.collection;

import org.junit.Assert;
import org.junit.Test;

public final class PairTest {

    @Test
    public void testPair() {
        Pair<String, Integer> pair = new Pair<>();

        Assert.assertNull(pair.getFirst());
        Assert.assertNull(pair.getSecond());

        pair.setFirst("Test");
        pair.setSecond(5);

        Assert.assertNotNull(pair.getFirst());
        Assert.assertNotNull(pair.getSecond());

        Assert.assertEquals("Test", pair.getFirst());
        Assert.assertEquals(5, pair.getSecond().intValue());

        pair = new Pair<>("foobar", 44);

        Assert.assertNotNull(pair.getFirst());
        Assert.assertNotNull(pair.getSecond());

        Assert.assertEquals("foobar", pair.getFirst());
        Assert.assertEquals(44, pair.getSecond().intValue());
    }
}