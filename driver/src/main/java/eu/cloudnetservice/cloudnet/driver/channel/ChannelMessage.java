/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.channel;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.DriverEnvironment;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessageTarget.Type;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.provider.CloudMessenger;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import java.util.ArrayList;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a message object which can be sent over the network with specific targets in mind. Unlike direct packet
 * communication, channel messages are not bound to a specific messaging channels but can rather get sent to all
 * components which are somewhere connected in the network. This means that it is possible to send a channel message to
 * a service which is running on another node than the current service which is sending the channel message.
 * <p>
 * A channel message has two main identification points. One is the channel to which the message gets sent. The channel
 * is a string object which is generally used to collect multiple types of channel message to a collection of message
 * types. On the other hand a channel message contains a message object which should be unique within in the network and
 * is used to identify a channel message in a group messages sent to the same channel.
 * <p>
 * The message contains a {@link DataBuf} containing the actual content of the message. There is no real way to identify
 * which types are in the buffer or not, therefore it is crucial that a channel message gets identified via its channel
 * and/or message.
 * <p>
 * If targets were given that are not locatable in the network they will get ignored silently. On the other hand this
 * means that if you try to send a channel message to a non-existing target, the send method will block until the future
 * wait timeout (30 seconds by default) expired before returning.
 * <p>
 * Note: there is no guarantee that the sender of a channel message is the actual component sending the message, as the
 * message can be modified on its way to the receiver.
 * <p>
 * The actual constructor of this class shouldn't get used. Use {@link #builder()} instead.
 *
 * @param prioritized whether this channel message should be handled with priority over other channel messages.
 * @param channel     the channel to which the channel message gets sent. Mostly for identification reasons.
 * @param message     the message key of this channel message. Mostly for identification reasons.
 * @param content     the content of this channel message (the actual data to send).
 * @param sender      the sender of the channel message. Should be, but must not the current network component.
 * @param targets     the targets to which the channel message should get send.
 * @see ChannelMessageSender
 * @see ChannelMessageTarget
 * @see eu.cloudnetservice.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent
 * @since 4.0
 */
public record ChannelMessage(
  boolean prioritized,
  @NonNull String channel,
  @NonNull String message,
  @NonNull DataBuf content,
  @NonNull ChannelMessageSender sender,
  @NonNull Collection<ChannelMessageTarget> targets
) {

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
   * Constructs a new builder which contains all needed data to respond to a channel message. As the channel message
   * will get directly handled by the waiting future, there is no need to actually set the channel and message of the
   * returned builder. The new builder will target the sender of the given input and has no data set.
   *
   * @param input the channel message to respond to.
   * @return a new builder for a channel message holding all base information to respond to the given source message.
   * @throws NullPointerException if the given input is null.
   */
  @Contract("_ -> new")
  public static @NonNull Builder buildResponseFor(@NonNull ChannelMessage input) {
    return builder().channel("").message("").target(input.sender.type(), input.sender.name());
  }

  /**
   * Sends this channel message using the current messenger of the environment. This is in fact just a shortcut method
   * for {@link CloudMessenger#sendChannelMessage(ChannelMessage)}. This method will not wait for the target component
   * to respond (it doesn't even expect a response) but for the handling component to send the message.
   *
   * @see CloudNetDriver#messenger()
   */
  public void send() {
    this.messenger().sendChannelMessage(this);
  }

  /**
   * Sends this channel message as a query and returns a future which waits for target component(s) to respond. This
   * method is a shortcut for {@link CloudMessenger#sendChannelMessageQueryAsync(ChannelMessage)}. The future will be
   * completed when the target component responds or the query future times out (after 30 seconds).
   *
   * @return a future completed with all responses of all target components of this channel message.
   * @see CloudNetDriver#messenger()
   */
  public @NonNull Task<Collection<ChannelMessage>> sendQueryAsync() {
    return this.messenger().sendChannelMessageQueryAsync(this);
  }

  /**
   * Sends this channel message as a query and returns a future which waits for target component(s) to respond. Only the
   * first response of any target will get sent back to this component. This is in particular useful if there is only
   * one target, or you are only expecting one component of the target components to respond. This is in fact just a
   * shortcut method for {@link CloudMessenger#sendSingleChannelMessageQueryAsync(ChannelMessage)}. The future will be
   * completed when one target component responds or the query future times out (after 30 seconds).
   *
   * @return a future completed with the first response of any target of this channel message.
   * @see CloudNetDriver#messenger()
   */
  public @NonNull Task<ChannelMessage> sendSingleQueryAsync() {
    return this.messenger().sendSingleChannelMessageQueryAsync(this);
  }

  /**
   * Sends this channel message as a query and suspends the calling thread until all responses is available or the query
   * timeout of 30 seconds is exceeded. This method is a shortcut for {@link CloudMessenger#sendChannelMessageQuery(ChannelMessage)}.
   *
   * @return all responses of all components this channel message is targeting.
   * @see CloudNetDriver#messenger()
   */
  public @NonNull Collection<ChannelMessage> sendQuery() {
    return this.messenger().sendChannelMessageQuery(this);
  }

  /**
   * Sends this channel message as a query and returns and blocks until one of the target component responded to this
   * message or the query timeout of 30 seconds is exceeded. This is in particular useful if there is only one target,
   * or you are only expecting one component of the target components to respond. This is in fact just a shortcut method
   * for {@link CloudMessenger#sendSingleChannelMessageQueryAsync(ChannelMessage)}.
   *
   * @return the first response of any component this message is targeting.
   * @see CloudNetDriver#messenger()
   */
  public @Nullable ChannelMessage sendSingleQuery() {
    return this.messenger().sendSingleChannelMessageQuery(this);
  }

  /**
   * Util method to get the current messenger of the environment.
   *
   * @return the current messenger of the environment.
   * @see CloudNetDriver#messenger()
   */
  private @NonNull CloudMessenger messenger() {
    return CloudNetDriver.instance().messenger();
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
   * If no sender of the message is given the current network component will be used as the sender of the message.
   */
  public static final class Builder {

    private final Collection<ChannelMessageTarget> targets = new ArrayList<>();

    private String channel;
    private String message;

    private boolean prioritized;

    private DataBuf content;
    private ChannelMessageSender sender;

    /**
     * Sets the sender of this message. If no sender is given the current component will be used as the sender. Note
     * that if you change the sender and try to receive a query response it will send the response to the given sender,
     * not this component.
     *
     * @param sender the sender of this message.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the given sender is null.
     * @see ChannelMessageSender#self()
     */
    public @NonNull Builder sender(@NonNull ChannelMessageSender sender) {
      this.sender = sender;
      return this;
    }

    /**
     * Sets the channel of this message. Might be empty but should be unique to identify within the network.
     *
     * @param channel the channel.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the given channel is null.
     */
    public @NonNull Builder channel(@NonNull String channel) {
      this.channel = channel;
      return this;
    }

    /**
     * Sets the message key of this message. Might be empty but should be unique to identify within the network.
     *
     * @param message the message key.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the given message is null.
     */
    public @NonNull Builder message(@NonNull String message) {
      this.message = message;
      return this;
    }

    /**
     * Sets if the channel message should be prioritized over other channel messages.
     * <p>
     * <strong>USE WITH CAUTION!</strong> This can cause other packet to get read and handled delayed. Use this option
     * only if you know what you're doing and are absolutely sure that the packet is urgent for CloudNet to work for as
     * expected. Otherwise, don't touch this method.
     *
     * @param prioritized if the channel message is prioritized
     * @return the same builder as used to call the method, for chaining.
     */
    @Experimental
    public @NonNull Builder prioritized(boolean prioritized) {
      this.prioritized = prioritized;
      return this;
    }

    /**
     * Sets the content of this message. If no content was given an empty buffer will be used.
     *
     * @param dataBuf the content.
     * @return the same builder as used to call the method, for chaining.
     */
    public @NonNull Builder buffer(@Nullable DataBuf dataBuf) {
      this.content = dataBuf;
      return this;
    }

    /**
     * Adds the given target as a target of this message.
     *
     * @param target the target to add.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the given target is null.
     */
    public @NonNull Builder target(@NonNull ChannelMessageTarget target) {
      this.targets.add(target);
      return this;
    }

    /**
     * Adds a channel message target to this message. You may not target an environment using this method.
     *
     * @param type the type of the receiver.
     * @param name the name of the receiver.
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalArgumentException if type is {@link Type#ENVIRONMENT}
     * @throws NullPointerException     if the given target type is null.
     * @see ChannelMessageTarget#of(Type, String)
     */
    public @NonNull Builder target(@NonNull ChannelMessageTarget.Type type, @Nullable String name) {
      return this.target(ChannelMessageTarget.of(type, name));
    }

    /**
     * Adds a channel message target to this message. You may not target an environment using this method.
     *
     * @param environment the driver environment to target.
     * @param name        the name of the target, might be null to target all components with the given environment.
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalArgumentException if type is {@link Type#ENVIRONMENT}
     * @throws NullPointerException     if the given environment is null.
     * @see ChannelMessageTarget#of(Type, String)
     */
    public @NonNull Builder target(@NonNull DriverEnvironment environment, @Nullable String name) {
      return this.target(environment == DriverEnvironment.NODE
        ? ChannelMessageTarget.Type.NODE
        : ChannelMessageTarget.Type.SERVICE, name);
    }

    /**
     * Targets all components with the given type. You may not target an environment using this method.
     *
     * @param type the type of the receivers to target.
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalArgumentException if type is {@link Type#ENVIRONMENT}
     * @throws NullPointerException     if the given target type is null.
     * @see ChannelMessageTarget#of(Type, String)
     */
    public @NonNull Builder targetAll(@NonNull ChannelMessageTarget.Type type) {
      return this.target(type, null);
    }

    /**
     * Targets all components within the network.
     *
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalArgumentException if type is {@link Type#ENVIRONMENT}
     * @see ChannelMessageTarget#of(Type, String)
     */
    public @NonNull Builder targetAll() {
      return this.target(ChannelMessageTarget.Type.ALL, null);
    }

    /**
     * Targets all services within the network.
     *
     * @return the same builder as used to call the method, for chaining.
     * @throws IllegalArgumentException if type is {@link Type#ENVIRONMENT}
     * @see ChannelMessageTarget#of(Type, String)
     */
    public @NonNull Builder targetServices() {
      return this.targetAll(ChannelMessageTarget.Type.SERVICE);
    }

    /**
     * Targets a specific service in the network.
     *
     * @param name the name of the service to target.
     * @return the same builder as used to call the method, for chaining.
     */
    public @NonNull Builder targetService(@Nullable String name) {
      return this.target(ChannelMessageTarget.Type.SERVICE, name);
    }

    /**
     * Targets all services of the given task within the network.
     *
     * @param name the name of the task to target.
     * @return the same builder as used to call the method, for chaining.
     */
    public @NonNull Builder targetTask(@Nullable String name) {
      return this.target(ChannelMessageTarget.Type.TASK, name);
    }

    /**
     * Targets a specific node within the network.
     *
     * @param name the name of the node to target.
     * @return the same builder as used to call the method, for chaining.
     */
    public @NonNull Builder targetNode(@Nullable String name) {
      return this.target(ChannelMessageTarget.Type.NODE, name);
    }

    /**
     * Targets all nodes within the network.
     *
     * @return the same builder as used to call the method, for chaining.
     */
    public @NonNull Builder targetNodes() {
      return this.targetAll(ChannelMessageTarget.Type.NODE);
    }

    /**
     * Targets all services with the given environment within the network.
     *
     * @param environment the environment to target.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the given environment is null.
     */
    public @NonNull Builder targetEnvironment(@NonNull ServiceEnvironmentType environment) {
      return this.target(ChannelMessageTarget.environment(environment));
    }

    /**
     * Builds a channel message from this builder within the contract given in the Builder class java docs.
     *
     * @return the created channel message from this builder.
     * @throws NullPointerException     if no message or channel is provided.
     * @throws IllegalArgumentException if no target was specified.
     */
    @Contract(" -> new")
    public @NonNull ChannelMessage build() {
      Preconditions.checkNotNull(this.channel, "No channel provided");
      Preconditions.checkNotNull(this.message, "No message provided");
      Preconditions.checkArgument(!this.targets.isEmpty(), "No targets provided");

      return new ChannelMessage(
        this.prioritized,
        this.channel,
        this.message,
        this.content == null ? DataBuf.empty() : this.content,
        this.sender == null ? ChannelMessageSender.self() : this.sender,
        this.targets);
    }
  }
}
