package de.dytanic.cloudnet.common.concurrent.function;

@FunctionalInterface
public interface ThrowableConsumer<E, T extends Throwable> {

    void accept(E element) throws T;

}
