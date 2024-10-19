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

package eu.cloudnetservice.driver.impl.event;

import dev.derklaro.aerogel.Element;
import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventListenerException;
import eu.cloudnetservice.driver.event.InvocationOrder;
import eu.cloudnetservice.driver.event.RegisteredEventListener;
import eu.cloudnetservice.driver.inject.InjectUtil;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import java.lang.reflect.Method;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of a registered event listener.
 *
 * @since 4.0
 */
final class DefaultRegisteredEventListener implements RegisteredEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRegisteredEventListener.class);

  private final Object instance;
  private final Class<?> eventClass;
  private final EventListener eventListener;

  private final String methodName;
  private final Element[] methodArguments;
  private final MethodAccessor<?> methodAccessor;

  private final InjectionLayer<?> injectionLayer;

  /**
   * Constructs a new default registered event listener instance.
   *
   * @param instance       the instance all event listener methods should get called on.
   * @param targetMethod   the method of the event listener.
   * @param eventListener  the annotation used to identify the target method.
   * @param injectionLayer the injection layer to use when additional parameters are present on the target method.
   * @throws NullPointerException if one of the given arguments is null.
   */
  DefaultRegisteredEventListener(
    @NonNull Object instance,
    @NonNull Method targetMethod,
    @NonNull EventListener eventListener,
    @NonNull InjectionLayer<?> injectionLayer
  ) {
    // listener info
    this.instance = instance;
    this.eventListener = eventListener;
    this.injectionLayer = injectionLayer;

    // method information
    this.methodName = targetMethod.getName();
    this.eventClass = targetMethod.getParameterTypes()[0];

    // method access
    var reflexion = Reflexion.onBound(instance);
    this.methodAccessor = reflexion.unreflect(targetMethod);

    // injection stuff, ignore the first element (the event itself)
    this.methodArguments = InjectUtil.buildElementsForParameters(targetMethod.getParameters(), 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fireEvent(@NonNull Event event) {
    LOGGER.debug(
      "Calling event {} on listener {}",
      event.getClass().getName(),
      this.instance().getClass().getName());

    // find the parameter instances, set the first argument to the event instance
    var instances = InjectUtil.findAllInstances(this.injectionLayer, this.methodArguments, 1);
    instances[0] = event;

    // invoke the event listener & rethrow any thrown exceptions wrapped
    var result = this.methodAccessor.invokeWithArgs(instances);
    if (result.wasExceptional()) {
      throw new EventListenerException(String.format(
        "Error while invoking event listener %s in class %s",
        this.methodName,
        this.instance.getClass().getName()
      ), result.getException());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String channel() {
    return this.eventListener.channel();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull InvocationOrder order() {
    return this.eventListener.order();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Object instance() {
    return this.instance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Class<?> eventClass() {
    return this.eventClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull EventListener eventListener() {
    return this.eventListener;
  }
}
