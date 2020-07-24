package de.dytanic.cloudnet.common.concurrent;

public interface ThrowableConsumer<E, T extends Throwable> {

    void accept(E element) throws T;

}
