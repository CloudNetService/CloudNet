package de.dytanic.cloudnet.common.concurrent;

public interface IVoidThrowableCallback<T> extends IThrowableCallback<T, Void> {

  @Override
  Void call(T t) throws Throwable;

}
