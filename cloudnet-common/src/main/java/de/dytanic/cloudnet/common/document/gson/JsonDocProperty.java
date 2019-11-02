package de.dytanic.cloudnet.common.document.gson;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by Tareko on 20.12.2017.
 */
public class JsonDocProperty<E> {

    protected final BiConsumer<E, JsonDocument> appender;

    protected final Function<JsonDocument, E> resolver;

    protected final Consumer<JsonDocument> remover;

    protected final Predicate<JsonDocument> tester;

    public JsonDocProperty(BiConsumer<E, JsonDocument> appender, Function<JsonDocument, E> resolver, Consumer<JsonDocument> remover, Predicate<JsonDocument> tester) {
        this.appender = appender;
        this.resolver = resolver;
        this.remover = remover;
        this.tester = tester;
    }

    public BiConsumer<E, JsonDocument> getAppender() {
        return this.appender;
    }

    public Function<JsonDocument, E> getResolver() {
        return this.resolver;
    }

    public Consumer<JsonDocument> getRemover() {
        return this.remover;
    }

    public Predicate<JsonDocument> getTester() {
        return this.tester;
    }
}