package de.dytanic.cloudnet.common.concurrent.function;

@FunctionalInterface
public interface ThrowableSupplier<O, T extends Throwable> {

  O get() throws T;

}
