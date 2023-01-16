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

package eu.cloudnetservice.driver.network.rpc.listener;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import eu.cloudnetservice.driver.network.rpc.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.driver.network.rpc.RPCInvocationContext;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.util.ExceptionalResultUtil;
import eu.cloudnetservice.driver.network.rpc.exception.CannotDecideException;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A network packet listener designed to handle all rpc messages using an underlying handler registry to post method
 * call instructions to it.
 *
 * @since 4.0
 */
@Singleton
public class RPCPacketListener implements PacketListener {

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
    // the result of the invocation, encoded
    DataBuf result = null;
    // the input information we get
    var buf = packet.content();
    // check if the invocation is chained
    if (buf.readBoolean()) {
      // get the chain size
      var chainSize = buf.readInt();
      // invoke the method on the current result
      RPCHandler.HandlingResult lastResult = null;
      for (var i = 1; i < chainSize; i++) {
        if (i == 1) {
          // always invoke the first method
          lastResult = this.handleRaw(buf.readString(), this.buildContext(channel, buf, null, false));
        } else if (lastResult != null) {
          if (lastResult.wasSuccessful()) {
            // only invoke upcoming methods if there was a previous result
            lastResult = this.handleRaw(
              buf.readString(),
              this.buildContext(channel, buf, lastResult.invocationResult(), true));
          } else {
            // an exception was thrown previously, break
            buf.readString(); // remove the handler information which is not necessary
            result = this.serializeResult(
              lastResult,
              lastResult.invocationHandler().dataBufFactory(),
              lastResult.invocationHandler().objectMapper(),
              this.buildContext(channel, buf, null, true));
            break;
          }
        } else {
          // just process over to remove the content from the buffer
          this.handleRaw(buf.readString(), this.buildContext(channel, buf, null, true));
        }
      }
      // check if there is already a result (which is caused by an exception - we can skip the handling step then)
      if (result == null && lastResult != null) {
        // the last handler decides over the method invocation result
        result = this.handle(
          buf.readString(),
          this.buildContext(channel, buf, lastResult.invocationResult(), true));
      }
    } else {
      // just invoke the method
      result = this.handle(buf.readString(), this.buildContext(channel, buf, null, false));
    }
    // check if we need to send a result
    if (result != null && packet.uniqueId() != null) {
      var response = new BasePacket(-1, result);
      response.uniqueId(packet.uniqueId());
      channel.sendPacket(response);
    }
  }

  /**
   * Posts the next rpc instruction in the given context into the handler for the given class which potentially contains
   * the target method and serializes the result into a data buffer. Null is returned when no handler for the given
   * class is present.
   *
   * @param clazz   the class in which the method to call is located.
   * @param context the context of the method invocation passed to the handler for the method invocation.
   * @return the serialized result of the method invocation, or null if no handler for the given class is registered.
   * @throws NullPointerException  if either the given class or invocation context is null.
   * @throws CannotDecideException if none or multiple methods are matching the method to call in the given class.
   */
  protected @Nullable DataBuf handle(@NonNull String clazz, @NonNull RPCInvocationContext context) {
    // get the handler associated with the class of the rpc
    var handler = this.rpcHandlerRegistry.handler(clazz);
    // check if the method gets called on a specific instance
    if (handler != null) {
      // invoke the method
      var handlingResult = handler.handle(context);
      // serialize the result
      return this.serializeResult(handlingResult, handler.dataBufFactory(), handler.objectMapper(), context);
    }
    // no handler for the class - no result
    return null;
  }

  /**
   * Serializes the given handling result into a newly allocated buffer using the given data buf factory. This method
   * returns null if the caller of this handler did not expect an invocation result.
   *
   * @param result         the result to serialize.
   * @param dataBufFactory the factory to use for buffer allocation.
   * @param objectMapper   the mapper to use for object serialization.
   * @param context        the invocation context of the invocation the result is getting serialized of.
   * @return the serialized invocation result or null if the calling method did not expect a result.
   * @throws NullPointerException if one of the given parameters is null.
   */
  protected @Nullable DataBuf serializeResult(
    @NonNull RPCHandler.HandlingResult result,
    @NonNull DataBufFactory dataBufFactory,
    @NonNull ObjectMapper objectMapper,
    @NonNull RPCInvocationContext context
  ) {
    // check if the sender expect the result of the method
    if (context.expectsMethodResult()) {
      // check if the method return void
      if (result.wasSuccessful() && result.targetMethodInformation().voidMethod()) {
        return dataBufFactory.createWithExpectedSize(2)
          .writeBoolean(true) // was successful
          .writeBoolean(false);
      } else if (result.wasSuccessful()) {
        // successful - write the result of the invocation
        return objectMapper.writeObject(dataBufFactory.createEmpty().writeBoolean(true), result.invocationResult());
      } else {
        // not successful - send some basic information about the result
        var throwable = (Throwable) result.invocationResult();
        return ExceptionalResultUtil.serializeThrowable(dataBufFactory.createEmpty().writeBoolean(false), throwable);
      }
    }
    // no result expected or no handler
    return null;
  }

  /**
   * Posts the next rpc instruction in the given context into the handler for the given class which potentially contains
   * the target method. Null is returned when no handler for the given class is present.
   *
   * @param clazz   the class in which the method to call is located.
   * @param context the context of the method invocation passed to the handler for the method invocation.
   * @return the result of the method invocation, or null if no handler for the given class is registered.
   * @throws NullPointerException  if either the given class or invocation context is null.
   * @throws CannotDecideException if none or multiple methods are matching the method to call in the given class.
   */
  protected @Nullable RPCHandler.HandlingResult handleRaw(
    @NonNull String clazz,
    @NonNull RPCInvocationContext context
  ) {
    // get the handler associated with the class of the rpc
    var handler = this.rpcHandlerRegistry.handler(clazz);
    // invoke the handler with the information
    return handler == null ? null : handler.handle(context);
  }

  /**
   * Builds a new context for a rpc method invocation based on the given information and remaining content in the
   * buffer. The given buffer should still contain (in the given order):
   * <ol>
   *   <li>the target method name
   *   <li>a boolean indicating if the rpc call expects a result
   *   <li>the number of arguments of the target method
   * </ol>
   *
   * @param channel             the network channel on which the rpc request was received.
   * @param content             the remaining buffer content, containing the data as described above.
   * @param on                  the object to call the method on, when using a rpc chain.
   * @param strictInstanceUsage if using the instance provided to the context is required.
   * @return a generated invocation context based on the given information.
   * @throws NullPointerException if either the given channel or content buffer is null.
   */
  protected @NonNull RPCInvocationContext buildContext(
    @NonNull NetworkChannel channel,
    @NonNull DataBuf content,
    @Nullable Object on,
    boolean strictInstanceUsage
  ) {
    return RPCInvocationContext.builder()
      .workingInstance(on)
      .channel(channel)
      .methodName(content.readString())
      .expectsMethodResult(content.readBoolean())
      .argumentCount(content.readInt())
      .argumentInformation(content)
      .normalizePrimitives(Boolean.TRUE)
      .strictInstanceUsage(strictInstanceUsage)
      .build();
  }
}
