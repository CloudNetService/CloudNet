package de.dytanic.cloudnet.common.concurrent;

public interface ICallback<T, R> extends IThrowableCallback<T, R> {

  @Override
  R call(T call);
}