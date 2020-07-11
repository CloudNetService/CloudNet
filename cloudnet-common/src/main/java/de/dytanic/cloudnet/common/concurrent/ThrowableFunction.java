package de.dytanic.cloudnet.common.concurrent;

public interface ThrowableFunction<I, O, T extends Throwable> {

    O apply(I input) throws T;

}
