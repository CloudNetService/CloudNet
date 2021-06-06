package de.dytanic.cloudnet.common.concurrent.function;

@FunctionalInterface
public interface ThrowableFunction<I, O, T extends Throwable> {

  O apply(I i) throws T;

}
