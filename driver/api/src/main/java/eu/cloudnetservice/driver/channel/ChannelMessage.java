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

package eu.cloudnetservice.driver.channel;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.provider.CloudMessenger;
import java.io.Closeable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a message object that can be sent over the network with specific targets in mind. Unlike direct packet
 * communication, channel messages are not bound to specific messaging channels but can rather get sent to all
 * components that are somewhere connected in the network. This means that it is possible to send a channel message to a
 * service which is running on another node than the service which is sending the channel message.
 * <p>
 * A channel message has two main identification points. One is the channel to which the message gets sent. The channel
 * is a string generally used to group channel messages together. This is, for example, useful to identify all channel
 * messages that are sent by a specific module. Further narrowing of the message type is done by using the message key,
 * which should uniquely identify the specific message. Each channel message must be composed of a unique channel and
 * message to distinguish it from other messages being sent in the cluster.
 * <p>
 * The message contains a {@link DataBuf} containing the actual content of the message. There is no real way to identify
 * which types are in the buffer or not, therefore, it is crucial that a channel message gets identified via its channel
 * and message keys.
 * <p>
 * If targets were given that are not locatable in the network, they will get ignored silently.
 * <p>
 * Note: there is no guarantee that the sender of a channel message is the actual component sending the message, as the
 * message can be modified on its way to the receiver.
 * <p>
 * The actual constructor of this class shouldn't get used. Use {@link #builder()} instead.
 *
 * @param sendSync    whether sending the message should block the current thread until the message is flushed.
 * @param prioritized whether this channel message should be handled with priority over other channel messages.
 * @param channel     the channel to which the channel message gets sent. Mostly for identification reasons.
 * @param message     the message key of this channel message. Mostly for identification reasons.
 * @param content     the content of this channel message (the actual data to send).
 * @param sender      the sender of the channel message. Should be, but must not the current network component.
 * @param targets     the targets to which the channel message should get send.
 * @see ChannelMessageSender
 * @see ChannelMessageTarget
 * @see ChannelMessageReceiveEvent
 * @since 4.0
 */
public record ChannelMessage(
  boolean sendSync,
  boolean prioritized,
  @NonNull String channel,
  @NonNull String message,
  @NonNull DataBuf content,
  @NonNull ChannelMessageSender sender,
  @NonNull Collection<ChannelMessageTarget> targets
) implements AutoCloseable {

  /**
   * Constructs a new, empty builder for a ChannelMessage.
   *
   * @return a new, empty builder for a ChannelMessage.
   */
  @Contract(" -> new")
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder which contains all necessary data to respond to a channel message. As the channel message
   * will get directly handled by the waiting future, there is no need to actually set the channel and message of the
   * returned builder. The new builder will target the sender of the given input and has no data set.
   *
   * @param input the channel message to respond to.
   * @return a new builder for a channel message holding all base information to respond to the given source message.
   * @throws NullPointerException if the given input is null.
   */
  @Contract("_ -> new")
  public static @NonNull Builder buildResponseFor(@NonNull ChannelMessage input) {
    return builder().channel("").message("").target(input.sender.toTarget());
  }

  /**
   * Sends this channel message using the current messenger of the environment. This is a shortcut method for
   * {@link CloudMessenger#sendChannelMessage(ChannelMessage)}. This method will not wait for the target component to
   * respond (it doesn't even expect a response) but for the handling component to send the message.
   */
  public void send() {
    this.messenger().sendChannelMessage(this);
  }

  /**
   * Sends this channel message as a query and returns a future which waits for target component(s) to respond. This
   * method is a shortcut for {@link CloudMessenger#sendChannelMessageQueryAsync(ChannelMessage)}. The future will be
   * completed when the target component responds or the query future times out.
   *
   * @return a future completed with all responses of all components targeted by this channel message.
   */
  public @NonNull CompletableFuture<Collection<ChannelMessage>> sendQueryAsync() {
    return this.messenger().sendChannelMessageQueryAsync(this);
  }

  /**
   * Sends this channel message as a query and returns a future which waits for target component(s) to respond. Only the
   * first response of any target will get sent back to this component. This is in particular useful if there is only
   * one target, or you are only expecting one of the target components to respond. This is a shortcut method for
   * {@link CloudMessenger#sendSingleChannelMessageQueryAsync(ChannelMessage)}. The future will be completed when one
   * target responds or the query future times out.
   *
   * @return a future completed with the first response of any target of this channel message.
   */
  public @NonNull CompletableFuture<ChannelMessage> sendSingleQueryAsync() {
    return this.messenger().sendSingleChannelMessageQueryAsync(this);
  }

  /**
   * Sends this channel message as a query and suspends the calling thread until all responses are available or the
   * query timeout is exceeded. This method is a shortcut for
   * {@link CloudMessenger#sendChannelMessageQuery(ChannelMessage)}.
   *
   * @return all responses of all components this channel message is targeting.
   */
  public @NonNull Collection<ChannelMessage> sendQuery() {
    return this.messenger().sendChannelMessageQuery(this);
  }

  /**
   * Sends this channel message as a query and returns and blocks until one of the target component responded to this
   * message or the query timeout is exceeded. This is in particular useful if there is only one target, or you are only
   * expecting one of the target components to respond. This is a shortcut method for
   * {@link CloudMessenger#sendSingleChannelMessageQueryAsync(ChannelMessage)}.
   *
   * @return the first response of any component this message is targeting.
   */
  public @Nullable ChannelMessage sendSingleQuery() {
    return this.messenger().sendSingleChannelMessageQuery(this);
  }

  /**
   * Util method to get the current messenger of the environment.
   *
   * @return the current messenger of the environment.
   */
  @ApiStatus.Internal
  private @NonNull CloudMessenger messenger() {
    return InjectionLayer.boot().instance(CloudMessenger.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    this.content.close();
  }

  /**
   * A builder for a channel message. This class should be used over direct constructor access as allows better
   * customization and validation of a channel message which gets created. Required properties are:
   * <ul>
   *   <li>channel, can be empty
   *   <li>message, can be empty
   *   <li>at least one target for the message
   * </ul>
   * <p>
   * If no sender for the message is given, the current network component will be used as the sender of the message.
   * The {@link #build()} method can only be called once for a channel message builder instance.
   *
   * @since 4.0
   */
  public static final class Builder implements Closeable {

    private static final VarHandle BUILD_CALLED;

    static {
      try {
        var lookup = MethodHandles.lookup();
        BUILD_CALLED = lookup.findVarHandle(Builder.class, "buildCalled", boolean.class);
      } catch (NoSuchFieldException | IllegalAccessException exception) {
        throw new ExceptionInInitializerError(exception);
      }
    }

    private final Collection<ChannelMessageTarget> targets = new HashSet<>();

    private String channel;
    private String message;

    private boolean sendSync;
    private boolean prioritized;

    private DataBuf content;
    private ChannelMessageSender sender;

    // internal marker to ensure that build() is only called once per builder instance
    // this is due to the content data buf, it cannot be shared between multiple channel message instances
    @SuppressWarnings("FieldMayBeFinal") // modified by BUILD_CALLED in build()
    private volatile boolean buildCalled = false;

    /**
     * Constructs a new builder instance. Use {@link ChannelMessage#builder()} instead.
     */
    private Builder() {
    }

    /**
     * Sets the sender of this message. If no sender is given, the current component will be used as the sender.
     *
     * @param sender the sender of this message.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException  if the given sender is null.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder sender(@NonNull ChannelMessageSender sender) {
      this.assertValidState();
      this.sender = sender;
      return this;
    }

    /**
     * Sets the channel of this message. The channel is primarily intended to group channel message of, for example, the
     * same module. This makes it much easier for receivers to assess whether a message should get handled by them. It
     * can be empty but should be unique to identify for the receiver.
     *
     * @param channel the channel of this message.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException  if the given channel is null.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder channel(@NonNull String channel) {
      this.assertValidState();
      this.channel = channel;
      return this;
    }

    /**
     * Sets the message key of this message. The key is primarily intended to uniquely identify one specific message to
     * the receiver. It can be empty but should be unique to identify for the receiver.
     *
     * @param message the message key.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException  if the given message is null.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder message(@NonNull String message) {
      this.assertValidState();
      this.message = message;
      return this;
    }

    /**
     * Sets if the channel message should get send sync, blocking the sending thread until the message was written and
     * flushed through the network layer.
     *
     * @param sync if the message should get send sync.
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder sendSync(boolean sync) {
      this.assertValidState();
      this.sendSync = sync;
      return this;
    }

    /**
     * Sets if the channel message should get prioritized processing on the receiving components.
     * <p>
     * <strong>USE WITH CAUTION!</strong> This can cause other lags and delays in the network handling of the
     * receivers. Use this option only if you know what you're doing and are sure that the packet is urgent for CloudNet
     * to work for as expected. Otherwise, don't touch this method.
     *
     * @param prioritized if the channel message should get prioritized processing on the receiving components.
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    @ApiStatus.Experimental
    public @NonNull Builder prioritized(boolean prioritized) {
      this.assertValidState();
      this.prioritized = prioritized;
      return this;
    }

    /**
     * Sets the content of this message. If no content was given, an empty buffer will be used. Note that any previously
     * supplied content buffer won't be released when this method is called multiple times on the same builder.
     *
     * @param dataBuf the content of the message.
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder buffer(@Nullable DataBuf dataBuf) {
      this.assertValidState();
      this.content = dataBuf;
      return this;
    }

    /**
     * Adds the given channel message target as a target of this message.
     *
     * @param target the target to add.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException  if the given target is null.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder target(@NonNull ChannelMessageTarget target) {
      this.assertValidState();
      this.targets.add(target);
      return this;
    }

    /**
     * Targets all components within the network.
     *
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract(" -> this")
    public @NonNull Builder targetAll() {
      return this.target(ChannelMessageTarget.all());
    }

    /**
     * Targets all nodes within the network.
     *
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract(" -> this")
    public @NonNull Builder targetNodes() {
      return this.target(ChannelMessageTarget.allNodes());
    }

    /**
     * Targets all services within the network.
     *
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract(" -> this")
    public @NonNull Builder targetServices() {
      return this.target(ChannelMessageTarget.allServices());
    }

    /**
     * Targets a specific node within the network.
     *
     * @param nodeId the id of the node to target.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException  if the given node id is null.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder targetNode(@NonNull String nodeId) {
      return this.target(ChannelMessageTarget.node(nodeId));
    }

    /**
     * Targets a specific service in the network.
     *
     * @param serviceName the name of the service to target.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException  if the given service name is null.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder targetService(@NonNull String serviceName) {
      return this.target(ChannelMessageTarget.service(serviceName));
    }

    /**
     * Targets all services of the given task within the network.
     *
     * @param taskName the name of the task to target.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException  if the given task name is null.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder targetServicesOfTask(@NonNull String taskName) {
      return this.target(ChannelMessageTarget.servicesByTask(taskName));
    }

    /**
     * Targets all services of the given group within the network.
     *
     * @param groupName the name of the group to target.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException  if the given group name is null.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder targetServicesOfGroup(@NonNull String groupName) {
      return this.target(ChannelMessageTarget.servicesByGroup(groupName));
    }

    /**
     * Targets all services with the given environment within the network.
     *
     * @param environmentName the name of the environment to target.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException  if the given environment name is null.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder targetServicesOfEnvironment(@NonNull String environmentName) {
      return this.target(ChannelMessageTarget.servicesByEnvironment(environmentName));
    }

    /**
     * Targets all services that have the given property key associated with any value within the network.
     *
     * @param propertyKey the key of the property that must be associated on target services.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException  if the given property key is null.
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Contract("_ -> this")
    public @NonNull Builder targetServicesWithProperty(@NonNull String propertyKey) {
      return this.target(ChannelMessageTarget.servicesWithProperty(propertyKey));
    }

    /**
     * Builds a channel message from this builder.
     *
     * @return the created channel message from this builder.
     * @throws NullPointerException     if no message or channel is provided.
     * @throws IllegalArgumentException if no target was specified.
     * @throws IllegalStateException    if this method was called previously.
     */
    @Contract(" -> new")
    public @NonNull ChannelMessage build() {
      Preconditions.checkNotNull(this.channel, "No channel provided");
      Preconditions.checkNotNull(this.message, "No message provided");
      Preconditions.checkArgument(!this.targets.isEmpty(), "No targets provided");

      // ensure a valid state *after* the other preconditions, the caller
      // might recover or close this builder if any previous condition fails
      if (!BUILD_CALLED.compareAndSet(this, false, true)) {
        throw new IllegalStateException("ChannelMessage already built by this builder");
      }

      var content = Objects.requireNonNullElseGet(this.content, DataBuf::empty);
      var sender = Objects.requireNonNullElseGet(this.sender, ChannelMessageSender::self);
      return new ChannelMessage(
        this.sendSync,
        this.prioritized,
        this.channel,
        this.message,
        content,
        sender,
        this.targets);
    }

    /**
     * Closes the content buffer held by this builder. No further actions can be performed on this builder after this
     * message has been called. This method should be called in special cases when a channel message (and therefore the
     * content buffer) is no longer needed.
     *
     * @throws IllegalStateException if the {@link #build()} method was already called on this builder.
     */
    @Override
    public void close() {
      if (!BUILD_CALLED.compareAndSet(this, false, true)) {
        throw new IllegalStateException("ChannelMessage already built by this builder");
      }

      if (this.content != null) {
        this.content.release();
        this.content = null;
      }
    }

    /**
     * Ensures that the {@link #build()} method was not yet called on this builder.
     *
     * @throws IllegalStateException if the {@link #build()} method was already called.
     */
    private void assertValidState() {
      if (this.buildCalled) {
        throw new IllegalStateException("ChannelMessage already built by this builder");
      }
    }
  }
}
