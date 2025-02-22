/*
 * Copyright 2019-2024 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.driver.impl.registry;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.registry.ServiceRegistryRegistration;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A service registration that is just a proxy to another service registration and returns a proxy to that service
 * instance when the instance is retrieved. This way, for example, the default service registration can be updated
 * without having to get the default instance every time as the proxy just always delegates to the current default
 * registration.
 *
 * @param serviceType      the type of the service that is implemented by this registration.
 * @param binding          the binding in which this registration is registered.
 * @param delegateSupplier the supplier to get the service registration to which this registration delegates.
 * @param <S>              the type modeling the implemented service.
 * @since 4.0
 */
record ProxiedServiceRegistration<S>(
  @NonNull Class<S> serviceType,
  @NonNull ServiceRegistrationsBinding<S> binding,
  @NonNull Supplier<ServiceRegistryRegistration<S>> delegateSupplier
) implements ServiceRegistryRegistration<S> {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    var delegate = this.delegateSupplier.get();
    return delegate.name();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NonNull S serviceInstance() {
    var delegateRegistration = this.delegateSupplier.get();
    if (delegateRegistration instanceof NewInstanceServiceRegistration<S>) {
      // return a new service instance without proxying it in case the delegate creates a new instance
      // of some object per invocation. this service type should be used for instances holding some sort
      // of state, therefore we can't use a proxy which creates a new instance (and therefore a new state)
      // everytime some method is called on the delegate service implementation
      return delegateRegistration.serviceInstance();
    } else {
      // in case the current registration does not return a new instance per invocation we can return a
      // proxy that calls all invocations on the current service delegate. however, this proxy can only be
      // valid until the delegate is swapped with a delegate that returns a new instance per invocation, as
      // per the concerns outlined above. if the delegate type is swapped the proxy will throw an exception
      // when any method is called on it
      var handler = new DelegateToDefaultRegistrationHandler(() -> {
        var delegate = this.delegateSupplier.get();
        Preconditions.checkState(!(delegate instanceof NewInstanceServiceRegistration<?>), "invalid proxy delegate");
        return delegate.serviceInstance();
      });
      var proxy = Proxy.newProxyInstance(this.serviceType.getClassLoader(), new Class[]{this.serviceType}, handler);
      return (S) proxy;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean defaultService() {
    return true; // is always the default service
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void markAsDefaultService() {
    // is always the default service
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean valid() {
    // only becomes invalid if the binding becomes invalid, in all other
    // cases the binding must be able to provide a valid default registration
    return this.binding.valid();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean unregister() {
    var delegate = this.delegateSupplier.get();
    return this.binding.unregisterRegistration(delegate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull S get() {
    return this.serviceInstance();
  }

  private record DelegateToDefaultRegistrationHandler(
    @NonNull Supplier<Object> delegateSupplier
  ) implements InvocationHandler {

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nullable Object invoke(
      @NonNull Object proxy,
      @NonNull Method method,
      @Nullable Object[] args
    ) throws Throwable {
      var delegate = this.delegateSupplier.get();
      return method.invoke(delegate, args);
    }
  }
}
