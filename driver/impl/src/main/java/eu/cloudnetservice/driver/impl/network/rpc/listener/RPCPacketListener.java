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

package eu.cloudnetservice.driver.impl.network.rpc.listener;

import eu.cloudnetservice.driver.impl.network.rpc.handler.DefaultRPCInvocationContext;
import eu.cloudnetservice.driver.impl.network.rpc.handler.util.RPCExceptionUtil;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationContext;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A network packet listener designed to handle all rpc messages using an underlying handler registry to post method
 * call instructions to it.
 *
 * @since 4.0
 */
@Singleton
public final class RPCPacketListener implements PacketListener {

  private final RPCHandlerRegistry rpcHandlerRegistry;

  /**
   * Constructs a new rpc packet listener instance.
   *
   * @param rpcHandlerRegistry the registry to use to downstream call instructions to.
   * @throws NullPointerException if the given rpc handler registry is null.
   */
  @Inject
  public RPCPacketListener(@NonNull RPCHandlerRegistry rpcHandlerRegistry) {
    this.rpcHandlerRegistry = rpcHandlerRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) throws Exception {
    var content = packet.content();
    var resultExpected = packet.uniqueId() != null;

    try {
      var rpcDepth = content.readInt();
      if (rpcDepth <= 0) {
        // depth must be at least one (single call) or more (chained call)
        if (resultExpected) {
          var resultContent = DataBufFactory.defaultFactory()
            .createWithExpectedSize(1)
            .writeByte(RPCInvocationResult.STATUS_BAD_REQUEST)
            .writeString("invalid chain length");
          this.sendResponseData(channel, packet, resultContent);
        }
        return;
      }

      if (rpcDepth > 1) {
        // RPC chain, start executing the first step
        this.executeRPCChainStep(rpcDepth, 1, resultExpected, content, packet, channel, null);
      } else {
        // single method rpc, execute & respond if requested
        var targetClassName = content.readString();
        var invocationContext = this.buildContext(content, null);
        var handlingTask = this.postRPCRequestToHandler(targetClassName, invocationContext);
        if (resultExpected) {
          this.waitForInvocationCompletion(handlingTask, result -> {
            var resultContent = this.serializeHandlingResult(result);
            this.sendResponseData(channel, packet, resultContent);
          });
        }
      }
    } finally {
      // specifically release the buffer here to prevent memory leaks, especially if we didn't consume
      // the whole buffer content (for example due to an exception during handling)
      content.forceRelease();
    }
  }

  /**
   * Waits for the given invocation task to finish, calling the callback if that is the case. The given callback
   * receives a null value if the given task is null, the task completes exceptionally or with a value of null.
   *
   * @param invocationTask the task representing a method invocation process.
   * @param callback       the callback to call when the invocation finished.
   * @throws NullPointerException if the given callback is null.
   */
  private void waitForInvocationCompletion(
    @Nullable CompletableFuture<RPCInvocationResult> invocationTask,
    @NonNull Consumer<RPCInvocationResult> callback
  ) {
    if (invocationTask == null) {
      // nothing to wait for
      callback.accept(null);
    } else {
      // wait for the completion of the method
      invocationTask.whenComplete((result, _) -> callback.accept(result));
    }
  }

  /**
   * Executes the next RPC chain step of the current RPC chain.
   *
   * @param chainDepth                the full depth of the RPC chain.
   * @param currentDepth              the current depth the chain execution is at, starting at 1.
   * @param resultExpected            if the RPC invocation expects a result to be sent back.
   * @param content                   the data content of the RPC request.
   * @param request                   the request packet.
   * @param channel                   the network channel from which the request came.
   * @param previousMethodReturnValue the chain step invocation return value.
   * @throws NullPointerException if one of the required non-null arguments is null.
   */
  private void executeRPCChainStep(
    int chainDepth,
    int currentDepth,
    boolean resultExpected,
    @NonNull DataBuf content,
    @NonNull Packet request,
    @NonNull NetworkChannel channel,
    @Nullable Object previousMethodReturnValue
  ) {
    // execute the target method based on the provided input
    var targetClassName = content.readString();
    var invocationContext = this.buildContext(content, previousMethodReturnValue);
    var invocationTask = this.postRPCRequestToHandler(targetClassName, invocationContext);
    this.waitForInvocationCompletion(invocationTask, invocationResult -> {
      // handle the invocation result:
      //   -> continue invoking in case the invocation was successful and returned a non-null result
      //   -> send back an error when "null" pops up in the middle of the chain
      //   -> send back the result data in case it was the last invocation or an error occurred
      var stillWorkTodo = currentDepth != chainDepth;
      switch (invocationResult) {
        // set the previous result in case the result is non-null and is not the final invocation
        case RPCInvocationResult.Success(var result, _, _) when result != null && stillWorkTodo -> {
          var nextChainDepth = currentDepth + 1;
          this.executeRPCChainStep(chainDepth, nextChainDepth, resultExpected, content, request, channel, result);
        }
        // remap a successful "null" invocation in the middle of the chain to an error
        case RPCInvocationResult.Success(var result, var handler, var invokedMethod)
          when result == null && stillWorkTodo -> {
          if (resultExpected) {
            var msg = String.format(
              "Cannot invoke next method in chain because the return value of %s%s is null",
              invokedMethod.name(), invokedMethod.methodType());
            var exception = new NullPointerException(msg);
            exception.setStackTrace(RPCExceptionUtil.UNASSIGNED_STACK); // stack of this method has no useful info

            var remappedResult = new RPCInvocationResult.Failure(exception, handler, invokedMethod);
            var resultContent = this.serializeHandlingResult(remappedResult);
            this.sendResponseData(channel, request, resultContent);
          }
        }
        // send back a response in case it's the final invocation or the invocation yielded an error
        case null, default -> {
          if (resultExpected) {
            var resultContent = this.serializeHandlingResult(invocationResult);
            this.sendResponseData(channel, request, resultContent);
          }
        }
      }
    });
  }

  /**
   * Serializes the result of the RPC handling process into a data buffer.
   *
   * @param invocationResult the handling result to serialize.
   * @return a buffer containing the response content for the handling result.
   */
  private @NonNull DataBuf serializeHandlingResult(@Nullable RPCInvocationResult invocationResult) {
    return switch (invocationResult) {
      // the method invocation cannot be handled because the handler to call is not present
      case null -> DataBuf.empty()
        .writeByte(RPCInvocationResult.STATUS_BAD_REQUEST)
        .writeString("missing explicitly defined target handler to call");
      // handling was successful, serialize the result
      case RPCInvocationResult.Success(var result, _, _) -> {
        var objectMapper = invocationResult.invocationHandler().objectMapper();
        var baseResponseData = DataBuf.empty().writeByte(RPCInvocationResult.STATUS_OK);
        yield objectMapper.writeObject(baseResponseData, result);
      }
      // the method invocation cannot be handled because the handler to call is not present
      case RPCInvocationResult.Failure(var thrown, _, _) -> {
        var baseResponseData = DataBuf.empty().writeByte(RPCInvocationResult.STATUS_ERROR);
        yield RPCExceptionUtil.serializeHandlingException(baseResponseData, thrown);
      }
      // unable to invoke the target method due to a bad request from the client
      case RPCInvocationResult.BadRequest(var message, _) -> DataBuf.empty()
        .writeByte(RPCInvocationResult.STATUS_BAD_REQUEST)
        .writeString(message);
      // there was some sort of handling issue on the server side, not client related
      case RPCInvocationResult.ServerError(var message, _) -> DataBuf.empty()
        .writeByte(RPCInvocationResult.STATUS_SERVER_ERROR)
        .writeString(message);
    };
  }

  /**
   * Sends a serialized response to given request packet into the given network channel.
   *
   * @param channel  the channel to which the response should be sent.
   * @param request  the request to which a response is being sent.
   * @param response the encoded response data to send.
   * @throws NullPointerException if the given channel, request packet or response is null.
   */
  private void sendResponseData(@NonNull NetworkChannel channel, @NonNull Packet request, @NonNull DataBuf response) {
    var responsePacket = request.constructResponse(response);
    channel.sendPacket(responsePacket);
  }

  /**
   * Posts the given RPC invocation context to the RPC handler that is registered for the class with the given name. If
   * no handler is registered for the class, this methods returns null instead of an invocation result.
   *
   * @param targetClassName the name of class in which the method to call is located.
   * @param context         the invocation context holding the provided execution info from the remote.
   * @return the result of the method invocation, or null if no handler for the given class is registered.
   * @throws NullPointerException if either the given class name or invocation context is null.
   */
  // note: do not change this method name, it's used by RPCExceptionUtil.serializeHandlingException
  // to determine where the internal handling frame cutoff should be
  private @Nullable CompletableFuture<RPCInvocationResult> postRPCRequestToHandler(
    @NonNull String targetClassName,
    @NonNull RPCInvocationContext context
  ) {
    var handler = this.rpcHandlerRegistry.handler(targetClassName);
    return handler == null ? null : handler.handle(context);
  }

  /**
   * Builds a new context for a rpc method invocation based on the given information and remaining content in the
   * buffer. The given buffer should still contain (in the given order):
   * <ol>
   *   <li>the target method name.
   *   <li>the target method descriptor.
   *   <li>the argument information for the invocation, if any.
   * </ol>
   *
   * @param content         the remaining buffer content, containing the data as described above.
   * @param workingInstance the instance on which the methods should be called, null to use the handler binding.
   * @return a generated invocation context based on the given information.
   * @throws NullPointerException if the given content buffer is null.
   */
  private @NonNull RPCInvocationContext buildContext(@NonNull DataBuf content, @Nullable Object workingInstance) {
    // read data from buffer, this must be in order it's written to the buffer
    var methodName = content.readString();
    var methodDescriptor = content.readString();
    return new DefaultRPCInvocationContext.Builder()
      .methodName(methodName)
      .methodDescriptor(methodDescriptor)
      .argumentInformation(content) // might be unsafe, but we cannot slice the argument data due to the unknown size
      .workingInstance(workingInstance)
      .build();
  }
}
