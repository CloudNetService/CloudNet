package de.dytanic.cloudnet.common.concurrent;

public interface IThrowableCallback<T, R> {

  R call(T t) throws Throwable;

}