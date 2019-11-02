package de.dytanic.cloudnet.common.collection;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class IterablesTest {

    @Test
    public void testIterables() {
        Assert.assertNotNull(Iterables.newArrayList());
        Assert.assertNotNull(Iterables.newArrayList(Collections.singletonList(" ")));
        Assert.assertNotNull(Iterables.newConcurrentLinkedDeque());
        Assert.assertNotNull(Iterables.newConcurrentLinkedQueue());
        Assert.assertNotNull(Iterables.newHashSet());
        Assert.assertNotNull(Iterables.newLinkedList());
        Assert.assertNotNull(Iterables.newCopyOnWriteArrayList());

        Collection<String> collection = Iterables.newArrayList(Arrays.asList(
                "Ask",
                "Question",
                "Task",
                "Live",
                "Work",
                "Home",
                "Stairs",
                "Tables",
                "Horse",
                "Streams",
                "Cache",
                "Cloud",
                "Sky",
                "Bird",
                "Life"
        ));

        Assert.assertNotNull(collection);
        Assert.assertEquals(15, collection.size());

        Collection<Integer> integerCollection = Iterables.map(collection, s -> s.getBytes().length);

        Assert.assertEquals(collection.size(), integerCollection.size());

        for (String item : collection) {
            Assert.assertTrue(integerCollection.contains(item.getBytes().length));
        }

        Assert.assertTrue(Iterables.contains("Stairs", collection.toArray(new String[0])));
        Assert.assertEquals("Home", Iterables.first(collection.toArray(new String[0]), s -> s.startsWith("H")));

        Assert.assertEquals("Work", Iterables.first(collection, s -> s.startsWith("W")));

        Assert.assertEquals(1, Iterables.filter(collection, s -> s.equals("Home")).size());
    }
}