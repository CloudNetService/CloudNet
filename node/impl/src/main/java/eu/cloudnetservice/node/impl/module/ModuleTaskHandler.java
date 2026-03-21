/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module;

import com.google.common.base.Preconditions;
import dev.derklaro.aerogel.binding.key.BindingKey;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.node.module.ModuleState;
import eu.cloudnetservice.node.module.ModuleTask;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for module tasks per module, used to discover and execute the defined module tasks of a module.
 *
 * @since 4.0
 */
final class ModuleTaskHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModuleTaskHandler.class);

  // if null, the module tasks of the module main class were not discovered yet
  // if non-null, the module tasks were discovered and can be executed
  @VisibleForTesting
  Map<ModuleState, RegisteredModuleTask[]> tasksByState;

  /**
   * Discovers the module tasks in the given concrete class. This method can only be called if this task handler was not
   * yet initialized.
   *
   * @param moduleMainClass the module main class to discover the module tasks in.
   * @throws IllegalStateException    if this handler was already initialized.
   * @throws IllegalArgumentException if the given class is not a concrete class.
   */
  public void discoverModuleTasks(@NonNull Class<?> moduleMainClass) {
    Preconditions.checkState(this.tasksByState == null, "module tasks already discovered");
    Preconditions.checkArgument(!moduleMainClass.isArray(), "module main class must not be an array");
    Preconditions.checkArgument(!moduleMainClass.isInterface(), "cannot check for module tasks in interface");

    var visitingClass = moduleMainClass;
    var visistedMethodSet = new MethodSet();
    var discoveredModuleTasks = new HashMap<ModuleState, List<RegisteredModuleTask>>();
    do {
      var methods = visitingClass.getDeclaredMethods();
      for (var method : methods) {
        if (!Modifier.isStatic(method.getModifiers()) && !visistedMethodSet.add(method)) {
          LOGGER.debug("Skipping method {} because it was processed in a subclass already", method);
          continue;
        }

        var moduleTaskAnnotation = method.getAnnotation(ModuleTask.class);
        if (moduleTaskAnnotation == null) {
          LOGGER.debug("Skipping method {} because no @ModuleTask annotation is present on it", method);
          continue;
        }

        try {
          // ensure that the method is accessible for all future reflective invocations
          method.setAccessible(true);

          var paramBindingKeys = Arrays.stream(method.getParameters())
            .map(param -> BindingKey.of(param.getParameterizedType()).selectQualifier(param.getAnnotations()))
            .toArray(BindingKey<?>[]::new);
          var registeredTask = new RegisteredModuleTask(moduleTaskAnnotation.priority(), method, paramBindingKeys);
          for (var state : moduleTaskAnnotation.states()) {
            var stateTasks = discoveredModuleTasks.computeIfAbsent(state, _ -> new ArrayList<>());
            stateTasks.add(registeredTask);
            LOGGER.debug("Registered method {} as module task for state {}", method, state);
          }
        } catch (InaccessibleObjectException exception) {
          var msg = String.format(
            """
              Method %s (module main %s) cannot be made accessible. \
              Please make sure the defining method is public and/or exported/open (original reason: %s)
              """,
            method, moduleMainClass.getName(), exception.getMessage());
          throw new IllegalStateException(msg);
        }
      }

      visitingClass = visitingClass.getSuperclass();
    } while (visitingClass != null && visitingClass != Object.class);

    // map all the collected mappings again, but this time into a sorted array for ordered invocation
    Map<ModuleState, RegisteredModuleTask[]> tasksByState = HashMap.newHashMap(discoveredModuleTasks.size());
    for (var entry : discoveredModuleTasks.entrySet()) {
      var moduleTasks = entry.getValue();
      Collections.sort(moduleTasks);
      tasksByState.put(entry.getKey(), moduleTasks.toArray(RegisteredModuleTask[]::new));
    }

    // use a ro wrapper around the map, good enough for the internal state
    this.tasksByState = Collections.unmodifiableMap(tasksByState);
  }

  /**
   * Invokes the registered module tasks for the given state in the order defined by the user. If any of the task
   * methods throws an exception, the execution of subsequent methods is interrupted and the thrown exception is
   * returned. This handler must be initialized before this method can be executed.
   *
   * @param state                   the module state to invoke the task methods of.
   * @param moduleMainClassInstance the constructed main class instance of the module.
   * @param moduleInjectionLayer    the injection layer designated to the module.
   * @return the first exception thrown from any module task method, {@code null} if no task method throws.
   * @throws NullPointerException  if any of the given parameters is null.
   * @throws IllegalStateException if this task handler is not yet initialized.
   */
  public @Nullable Throwable invokeModuleTasks(
    @NonNull ModuleState state,
    @NonNull Object moduleMainClassInstance,
    @NonNull InjectionLayer<?> moduleInjectionLayer
  ) {
    Preconditions.checkState(this.tasksByState != null, "module tasks not yet discovered");

    var tasks = this.tasksByState.get(state);
    if (tasks == null) {
      return null;
    }

    for (var task : tasks) {
      var thrown = task.invoke(moduleMainClassInstance, moduleInjectionLayer);
      if (thrown != null) {
        return thrown;
      }
    }

    return null;
  }

  /**
   * Resets the internal state of this handler, as if no module tasks were discovered yet. This method does nothing if
   * no module tasks were discovered or if the state was already reset.
   */
  public void reset() {
    this.tasksByState = null;
  }

  /**
   * Wrapper around a registered module task method. This instance is sortable based on the given priority, a higher
   * priority lets them appear earlier in the sorted collection.
   *
   * @param priority         the priority of the module task method.
   * @param method           the actual module task method to invoke.
   * @param paramBindingKeys the injection binding keys for all parameters of the method.
   * @since 4.0
   */
  @VisibleForTesting
  record RegisteredModuleTask(
    byte priority,
    @NonNull Method method,
    @NonNull BindingKey<?>[] paramBindingKeys
  ) implements Comparable<RegisteredModuleTask> {

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * Invokes the wrapped method. The given instance is automatically ignored for static methods. All method parameter
     * values are resolved using the given injection layer. If an internal exception is caught (something not caused by
     * the method being invoked) it's wrapped and rethrown. Otherwise, the thrown exception is returned to the caller.
     *
     * @param moduleMainClassInstance the constructed main class instance of the module.
     * @param moduleInjectionLayer    the injection layer designated to the module.
     * @return the exception thrown by the method being invoked, if no exception was thrown {@code null} is returned.
     * @throws NullPointerException if any of the given parameters is null.
     */
    public @Nullable Throwable invoke(
      @NonNull Object moduleMainClassInstance,
      @NonNull InjectionLayer<?> moduleInjectionLayer
    ) {
      try {
        var parameters = this.resolveInvocationParameters(moduleInjectionLayer);
        var targetInstance = Modifier.isStatic(this.method.getModifiers()) ? null : moduleMainClassInstance;
        this.method.invoke(targetInstance, parameters);
        return null;
      } catch (IllegalAccessException exception) {
        // method is set accessible, so reaching here should never happen
        throw new AssertionError("Module task method is inaccessible, this should never happen");
      } catch (IllegalArgumentException | NullPointerException exception) {
        // thrown if a bad argument is provided to the invoke call
        var msg = String.format("BUG! Internal error while invoking module task method %s", this.method);
        throw new RuntimeException(msg, exception);
      } catch (ExceptionInInitializerError | InvocationTargetException exception) {
        // handles the exceptions caused by the method being invoked
        var msg = String.format("Caught exception while invoking module task method %s", this.method);
        return new RuntimeException(msg, exception.getCause());
      }
    }

    /**
     * Resolves the invocation parameters to use for the wrapped method using the given injection layer.
     *
     * @param moduleInjectionLayer the injection layer to use for resolving the method parameters.
     * @return the method parameters to use when invoking the method.
     * @throws NullPointerException if the given injection layer is null.
     */
    private @NonNull Object[] resolveInvocationParameters(@NonNull InjectionLayer<?> moduleInjectionLayer) {
      var paramCount = this.paramBindingKeys.length;
      if (paramCount == 0) {
        return EMPTY_OBJECT_ARRAY;
      }

      var parameters = new Object[paramCount];
      for (var index = 0; index < paramCount; index++) {
        var bindingKey = this.paramBindingKeys[index];
        parameters[index] = moduleInjectionLayer.instance(bindingKey);
      }

      return parameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@NonNull ModuleTaskHandler.RegisteredModuleTask other) {
      return Byte.compare(other.priority(), this.priority()); // reversed order to get high -> low
    }
  }

  /**
   * Set that holds name and parameter information about methods that were inserted into the set previously. This is
   * used for method override detection within a class. This implementation is not thread-safe.
   *
   * @since 4.0
   */
  private static final class MethodSet {

    private final Set<String> methodDescriptors = new HashSet<>();

    /**
     * Builds a descriptor string for the given method which includes the name and all parameter types.
     *
     * @param method the method to build the descriptor string for.
     * @return the descriptor string for the given method.
     * @throws NullPointerException if the given method is null.
     */
    private static @NonNull String buildDescriptorString(@NonNull Method method) {
      var paramJoiner = new StringJoiner("", "(", ")");
      for (var parameterType : method.getParameterTypes()) {
        paramJoiner.add(parameterType.descriptorString());
      }

      return method.getName() + paramJoiner;
    }

    /**
     * Adds the given method into this method set unless a method with the same descriptor was already added.
     *
     * @param method the method to add to this set.
     * @return {@code true} if the method was added to the set, {@code false} otherwise.
     */
    public boolean add(@NonNull Method method) {
      var descriptor = buildDescriptorString(method);
      return this.methodDescriptors.add(descriptor);
    }
  }
}
