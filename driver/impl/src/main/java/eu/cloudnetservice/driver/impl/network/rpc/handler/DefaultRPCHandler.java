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

package eu.cloudnetservice.driver.impl.network.rpc.handler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.cloudnetservice.driver.impl.network.rpc.DefaultRPCProvider;
import eu.cloudnetservice.driver.impl.network.rpc.handler.invoker.MethodInvoker;
import eu.cloudnetservice.driver.impl.network.rpc.handler.invoker.MethodInvokerGenerator;
import eu.cloudnetservice.driver.impl.network.rpc.introspec.DefaultRPCMethodMetadata;
import eu.cloudnetservice.driver.impl.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationContext;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationResult;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import io.vavr.control.Try;
import java.lang.constant.MethodTypeDesc;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the default implementation of a rpc handler.
 *
 * @since 4.0
 */
final class DefaultRPCHandler extends DefaultRPCProvider implements RPCHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRPCHandler.class);

  private final Object boundInstance;
  private final RPCClassMetadata targetClassMeta;
  private final Cache<DefaultRPCMethodMetadata, MethodInvoker> methodInvokerCache;

  /**
   * Constructs a new rpc handler instance.
   *
   * @param sourceFactory   the rpc factory that constructed this object.
   * @param objectMapper    the object mapper to use to write and read data from the buffers.
   * @param dataBufFactory  the buffer factory used for buffer allocations.
   * @param boundInstance   the instance to which this handler is bound, can be null.
   * @param targetClassMeta the metadata of the target class in which this handler should call methods.
   * @throws NullPointerException if any parameter, except the bound instance, is null.
   */
  DefaultRPCHandler(
    @NonNull RPCFactory sourceFactory,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory,
    @Nullable Object boundInstance,
    @NonNull RPCClassMetadata targetClassMeta
  ) {
    super(targetClassMeta.targetClass(), sourceFactory, objectMapper, dataBufFactory);
    this.boundInstance = boundInstance;
    this.targetClassMeta = targetClassMeta;
    this.methodInvokerCache = Caffeine.newBuilder()
      // remove invokers from cache after idling some time as they are defined as nested classes
      // which means that they can get garbage collected to free up some heap when there are
      // no more strong references (like the cache) to them anymore
      .expireAfterAccess(Duration.ofDays(1))
      .build();
  }

  /**
   * Tries to parse the given method descriptor, returning null if the given method descriptor is invalid.
   *
   * @param descriptor the descriptor that should be parsed.
   * @return the parsed method descriptor or null if the given descriptor is invalíd.
   * @throws NullPointerException if the given descriptor is null.
   */
  private static @Nullable MethodTypeDesc parseMethodDescriptor(@NonNull String descriptor) {
    try {
      return MethodTypeDesc.ofDescriptor(descriptor);
    } catch (IllegalArgumentException _) {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<RPCInvocationResult> handle(@NonNull RPCInvocationContext context) {
    // get the instance we're working with
    var contextualInstance = context.workingInstance();
    var workingInstance = contextualInstance != null ? contextualInstance : this.boundInstance;
    if (workingInstance == null) {
      return TaskUtil.finishedFuture(new RPCInvocationResult.ServerError("no instance to invoke the method on", this));
    }

    // parse the method type to validate it
    var targetMethodType = parseMethodDescriptor(context.methodDescriptor());
    if (targetMethodType == null) {
      return TaskUtil.finishedFuture(new RPCInvocationResult.BadRequest("invalid target method descriptor", this));
    }

    // find the associated method meta in the target class
    var targetMethod = this.targetClassMeta.findMethod(context.methodName(), targetMethodType);
    if (targetMethod == null) {
      return TaskUtil.finishedFuture(new RPCInvocationResult.BadRequest("target method not found", this));
    }

    // deserialize the provided method arguments, returns null in case the arguments buffer
    // has some invalid argument data at some position
    var methodArguments = this.deserializeMethodArguments(targetMethod, context.argumentInformation());
    if (methodArguments == null) {
      var targetDescriptor = targetMethod.methodType().descriptorString();
      var msg = String.format("provided arguments do not satisfy %s", targetDescriptor);
      return TaskUtil.finishedFuture(new RPCInvocationResult.BadRequest(msg, this));
    }

    // get or create a method invoker for the target method
    var maybeMethodInvoker = this.getOrCreateMethodInvoker(targetMethod);
    if (maybeMethodInvoker.isFailure()) {
      var constructionException = maybeMethodInvoker.getCause();
      LOGGER.error("unable to create method invoker for {}", targetMethod, constructionException);
      return TaskUtil.finishedFuture(new RPCInvocationResult.ServerError("unable to create method invoker", this));
    }

    try {
      var methodInvoker = maybeMethodInvoker.get();
      var invocationResult = methodInvoker.callMethod(workingInstance, methodArguments);
      if (invocationResult instanceof Future<?> future) {
        // future needs special care as we need to wait for the result to become available
        var futureCompletionStage = switch (future) {
          case CompletionStage<?> stage -> stage;
          case Future<?> rawFuture -> CompletableFuture.supplyAsync(() -> {
            try {
              return rawFuture.get();
            } catch (InterruptedException exception) {
              // reset interruption state
              Thread.currentThread().interrupt();
              throw new CompletionException("interrupt", exception);
            } catch (ExecutionException exception) {
              // wrap in completion exception, causes the future to complete with the raw exception
              throw new CompletionException(exception);
            }
          });
        };

        CompletableFuture<RPCInvocationResult> resultTask = new CompletableFuture<>();
        futureCompletionStage.whenComplete((futureResult, exception) -> {
          if (exception != null) {
            // method invocation failed with an exception
            resultTask.complete(new RPCInvocationResult.Failure(exception, this, targetMethod));
          } else {
            // method invocation completed successfully
            resultTask.complete(new RPCInvocationResult.Success(futureResult, this, targetMethod));
          }
        });
        return resultTask;
      } else {
        // result of method invocation is available immediately
        return TaskUtil.finishedFuture(new RPCInvocationResult.Success(invocationResult, this, targetMethod));
      }
    } catch (Throwable throwable) {
      return TaskUtil.finishedFuture(new RPCInvocationResult.Failure(throwable, this, targetMethod));
    }
  }

  /**
   * Gets an existing method invoker for the given target method from the cache or creates a new one. A failure is
   * returned in case a method invoker couldn't be generated for some reason.
   *
   * @param methodMetadata the metadata of the method to get or create the method invoker for.
   * @return a method invoker for the given target method.
   * @throws NullPointerException if the given method metadata is null.
   */
  private @NonNull Try<MethodInvoker> getOrCreateMethodInvoker(@NonNull DefaultRPCMethodMetadata methodMetadata) {
    return Try.of(() -> this.methodInvokerCache.get(methodMetadata, MethodInvokerGenerator::makeMethodInvoker));
  }

  /**
   * Deserializes the method arguments provided in the given buffer based on the generic parameter types provided from
   * the given target method metadata (in order). If an argument at a position is invalid (e.g. bad data or null for a
   * primitive) this method returns null.
   *
   * @param targetMethod           the target method to deserialize the method arguments for.
   * @param encodedArgumentsBuffer the buffer which holds the encoded method arguments to deserialize.
   * @return an array containing the deserialized method arguments, null if the buffer contained bad argument data.
   * @throws NullPointerException if the given target method or argument buffer is null.
   */
  private @Nullable Object[] deserializeMethodArguments(
    @NonNull DefaultRPCMethodMetadata targetMethod,
    @NonNull DataBuf encodedArgumentsBuffer
  ) {
    try {
      var paramTypes = targetMethod.parameterTypes();
      var methodArguments = new Object[paramTypes.length];
      for (var index = 0; index < paramTypes.length; index++) {
        var parameterType = paramTypes[index];
        var parameterValue = this.objectMapper.readObject(encodedArgumentsBuffer, parameterType);
        if (parameterType instanceof Class<?> clazz && clazz.isPrimitive() && parameterValue == null) {
          // primitive value can't be null, method invocation will fail
          return null;
        }

        methodArguments[index] = parameterValue;
      }

      return methodArguments;
    } catch (Exception _) {
      return null;
    }
  }
}
