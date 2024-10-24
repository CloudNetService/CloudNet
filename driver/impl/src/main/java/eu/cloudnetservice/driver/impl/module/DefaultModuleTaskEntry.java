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

package eu.cloudnetservice.driver.impl.module;

import dev.derklaro.aerogel.Element;
import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.driver.inject.InjectUtil;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.Module;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.ModuleTaskEntry;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import java.lang.reflect.Method;
import lombok.NonNull;

/**
 * Represents the default implementation of the module task entry.
 *
 * @see ModuleTaskEntry
 * @since 4.0
 */
public class DefaultModuleTaskEntry implements ModuleTaskEntry {

  /**
   * A simple format which allows unique method identifiers. Format: declaring class@method name()
   */
  protected static final String METHOD_SIGNATURE_FORMAT = "%s@%s()";

  // the underlying method we're calling
  protected final MethodAccessor<?> methodAccessor;

  // information for injection
  protected final Element[] paramTypes;
  protected final InjectionLayer<?> injectionLayer;

  // information about the task
  protected final ModuleTask moduleTask;
  protected final ModuleWrapper moduleWrapper;

  // debug stuff
  protected final String fullMethodSignatureCached;

  /**
   * Constructs a new instance of this class.
   *
   * @param wrapper the module wrapper associated with this task entry.
   * @param task    the module task annotation based on which this entry was created.
   * @param method  the method which was annotated with {@link ModuleTask}.
   * @throws IllegalAccessException if access checking for the provided method fails.
   * @throws NullPointerException   if wrapper, task or method is null.
   */
  public DefaultModuleTaskEntry(
    @NonNull ModuleWrapper wrapper,
    @NonNull ModuleTask task,
    @NonNull Method method
  ) throws IllegalAccessException {
    this.moduleTask = task;
    this.moduleWrapper = wrapper;
    this.injectionLayer = wrapper.injectionLayer();

    // init the method signature here as it cannot change anyway
    this.fullMethodSignatureCached = String.format(
      METHOD_SIGNATURE_FORMAT,
      method.getDeclaringClass().getCanonicalName(),
      method.getName());

    // init the method accessor
    var reflexion = Reflexion.onBound(this.module());
    this.methodAccessor = reflexion.unreflect(method);

    // parameter injection
    this.paramTypes = InjectUtil.buildElementsForParameters(method.getParameters());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Module module() {
    return this.moduleWrapper.module();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleWrapper moduleWrapper() {
    return this.moduleWrapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleTask taskInfo() {
    return this.moduleTask;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String fullMethodSignature() {
    return this.fullMethodSignatureCached;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fire() throws Throwable {
    // collect all the arguments we need to the method invocation
    var arguments = InjectUtil.findAllInstances(this.injectionLayer, this.paramTypes);

    // invoke the method, rethrow if there was an exception
    var result = this.methodAccessor.invokeWithArgs(arguments);
    if (result.wasExceptional()) {
      throw result.getException();
    }
  }
}
