/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.command;

/**
 * The commandSender represents the sender for a command dispatch operation
 */
public interface ICommandSender {

  /**
   * Returns the name of the command sender
   */
  String getName();

  /**
   * Sends a message to the specific sender implementation
   *
   * @param message that should send
   */
  void sendMessage(String message);

  /**
   * Send the messages to the specific sender implementation
   *
   * @param messages that should send
   */
  void sendMessage(String... messages);

  /**
   * Checks, that the commandSender has the permission to execute the command
   *
   * @param permission the permission, that should that
   * @return true if the command sender is authorized that it has the permission
   */
  boolean hasPermission(String permission);
}
