/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.http.annotation.parser;

import eu.cloudnetservice.common.StringUtil;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpHandler;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Collection;
import lombok.NonNull;

/**
 * A http handler which delegates its calls to a method invocation, resolving the parameters for that from the
 * preprocessed http context.
 *
 * @since 4.0
 */
final class MethodHttpHandlerInvoker extends HttpHandler {

  private final Object instance;
  private final MethodHandle handlerMethod;
  private final Class<?>[] handlerParameterTypes;
  private final Collection<String> supportedMethods;

  /**
   * Constructs a new MethodHttpHandlerInvoker instance.
   *
   * @param handlerInstance  the instance in which the http handler method is located.
   * @param handlerMethod    the method to delegate matching http calls to.
   * @param supportedMethods the supported http requests methods by this handler.
   * @throws NullPointerException   if the given instance, method or methods collection is null.
   * @throws IllegalAccessException if method access checking fails.
   */
  public MethodHttpHandlerInvoker(
    @NonNull Object handlerInstance,
    @NonNull Method handlerMethod,
    @NonNull Collection<String> supportedMethods
  ) throws IllegalAccessException {
    this.instance = handlerInstance;
    this.handlerMethod = genericHandleForMethod(handlerMethod); // TODO: reflexion (we can remove setAccessible then)
    this.handlerParameterTypes = handlerMethod.getParameterTypes();
    this.supportedMethods = supportedMethods;
  }

  /**
   * Converts the given method into a generic method handle which can be used to invoke the method without the exact
   * arguments or return type known to the caller.
   *
   * @param method the method to convert.
   * @return a method generic method handle for the given method.
   * @throws IllegalAccessException if method access checking fails.
   */
  private static @NonNull MethodHandle genericHandleForMethod(@NonNull Method method) throws IllegalAccessException {
    var methodType = MethodType.genericMethodType(1, true);
    return MethodHandles.lookup().unreflect(method)
      .asFixedArity()
      .asSpreader(1, Object[].class, method.getParameterCount())
      .asType(methodType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handle(@NonNull String path, @NonNull HttpContext context) throws Throwable {
    // check if this handler supports the request method, if not just ignore the request
    if (this.supportedMethods.contains(StringUtil.toUpper(context.request().method()))) {
      var arguments = this.buildInvocationArguments(path, context);
      this.handlerMethod.invoke(this.instance, arguments);
    }
  }

  /**
   * Constructs the arguments array which will be passed to the method which is actually handling the associated
   * request.
   *
   * @param path    the full path of the client request.
   * @param context the current context of the request.
   * @throws NullPointerException          if the given path or context is null.
   * @throws AnnotationHttpHandleException if any exception occurs while resolving the arguments.
   */
  private @NonNull Object[] buildInvocationArguments(@NonNull String path, @NonNull HttpContext context) {
    var arguments = new Object[this.handlerParameterTypes.length];
    var invocationHints = context.invocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY);

    // check if enough arguments are passed from the preprocessing (-1 as the context is always the first argument)
    var expectedArgumentCount = arguments.length - 1;
    if (invocationHints.size() != expectedArgumentCount) {
      throw new AnnotationHttpHandleException(path, String.format(
        "Arguments count to call handler does not match (got: %d; expected: %d)",
        invocationHints.size(),
        expectedArgumentCount));
    }

    // get the value of each hint and store it in the args array
    for (var invocationHint : invocationHints) {
      // validate that we got and invocation hint
      if (invocationHint instanceof ParameterInvocationHint hint) {
        // validate the index
        if (hint.index() <= 0 || hint.index() >= arguments.length) {
          throw new AnnotationHttpHandleException(path, "Invocation hint index " + hint.index() + " is out of bounds");
        }

        // validate that the value type is matching the expected type at the index
        var value = hint.resolveValue(path, context);
        var expectedType = this.handlerParameterTypes[hint.index()];
        if (value != null && !expectedType.isAssignableFrom(value.getClass())) {
          throw new AnnotationHttpHandleException(path, String.format(
            "Parameter at index %d is of type %s; expected type %s",
            hint.index(),
            value.getClass().getName(),
            expectedType.getName()));
        }

        // don't accidentally try to inject null into a primitive type
        if (value == null && expectedType.isPrimitive()) {
          throw new AnnotationHttpHandleException(path, String.format(
            "Parameter at index %d is primitive but null was resolved as the parameter value",
            hint.index()));
        }

        // all fine, store the argument
        arguments[hint.index()] = value;
      } else {
        throw new AnnotationHttpHandleException(path, "Hint " + invocationHint + " is not an ParameterInvocationHint");
      }
    }

    // put in the context argument and return the completed array
    arguments[0] = context;
    return arguments;
  }
}
