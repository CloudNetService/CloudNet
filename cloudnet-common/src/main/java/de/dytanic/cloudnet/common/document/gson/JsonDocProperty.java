package de.dytanic.cloudnet.common.document.gson;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by Tareko on 20.12.2017.
 */
@Getter
@AllArgsConstructor
public class JsonDocProperty<E> {

    protected final BiConsumer<E, JsonDocument> appender;

    protected final Function<JsonDocument, E> resolver;

    protected final Consumer<JsonDocument> remover;

    protected final Predicate<JsonDocument> tester;

}