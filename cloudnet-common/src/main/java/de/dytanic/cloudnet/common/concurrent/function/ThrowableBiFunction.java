package de.dytanic.cloudnet.common.concurrent.function;

@FunctionalInterface
public interface ThrowableBiFunction<I, U, O, T extends Throwable> {

  O apply(I i, U u) throws T;

}
