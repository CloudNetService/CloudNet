package de.dytanic.cloudnet.common.document.gson;

import de.dytanic.cloudnet.common.collection.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class JsonDocumentTest {

    @Test
    public void testDocument() {
        JsonDocument document = new JsonDocument();

        Assert.assertNotNull(document.append("foo", "bar"));

        document.append("number", 4).append("test", new TestClass("myData"));

        Assert.assertNotNull(document);
        Assert.assertEquals("bar", document.getString("foo"));
        Assert.assertEquals(4, document.getInt("number"));
        Assert.assertEquals("myData", document.get("test", TestClass.class).data);
        Assert.assertEquals("Hello, world!", new String(document.getBinary("test_binary", "Hello, world!".getBytes())));
    }

    @Test
    public void testProperties() {
        JsonDocProperty<Pair<String, String>> docProperty = new JsonDocProperty<Pair<String, String>>(

                new BiConsumer<Pair<String, String>, JsonDocument>() {
                    @Override
                    public void accept(Pair<String, String> stringStringPair, JsonDocument document) {
                        document
                                .append("firstProp", stringStringPair.getFirst())
                                .append("secondProp", stringStringPair.getSecond());
                    }
                },
                new Function<JsonDocument, Pair<String, String>>() {
                    @Override
                    public Pair<String, String> apply(JsonDocument document) {
                        if (!document.contains("firstProp") || !document.contains("secondProp")) return null;

                        return new Pair<>(document.getString("firstProp"), document.getString("secondProp"));
                    }
                },
                new Consumer<JsonDocument>() {
                    @Override
                    public void accept(JsonDocument document) {
                        document.remove("firstProp");
                        document.remove("secondProp");
                    }
                },
                new Predicate<JsonDocument>() {
                    @Override
                    public boolean test(JsonDocument jsonDocument) {
                        return jsonDocument.contains("firstProp") && jsonDocument.contains("secondProp");
                    }
                }
        );

        JsonDocument document = new JsonDocument();
        document.setProperty(docProperty, new Pair<>("foo", "bar"));

        Assert.assertTrue(document.hasProperty(docProperty));
        Assert.assertEquals("foo", document.getProperty(docProperty).getFirst());
        Assert.assertEquals("bar", document.getProperty(docProperty).getSecond());

        document.removeProperty(docProperty);

        Assert.assertEquals(0, document.size());
    }

    private class TestClass {
        private String data;

        public TestClass(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

}