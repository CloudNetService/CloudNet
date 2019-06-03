package de.dytanic.cloudnet.common.collection;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;

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

        Collection<Integer> integerCollection = Iterables.map(collection, new Function<String, Integer>() {
            @Override
            public Integer apply(String s) {
                return s.getBytes().length;
            }
        });

        Assert.assertEquals(collection.size(), integerCollection.size());

        for (String item : collection) Assert.assertTrue(integerCollection.contains(item.getBytes().length));

        Assert.assertTrue(Iterables.contains("Stairs", collection.toArray(new String[0])));
        Assert.assertEquals("Home", Iterables.first(collection.toArray(new String[0]), new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.startsWith("H");
            }
        }));

        Assert.assertEquals("Work", Iterables.first(collection, new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.startsWith("W");
            }
        }));

        Assert.assertEquals(1, Iterables.filter(collection, new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.equals("Home");
            }
        }).size());
    }
}