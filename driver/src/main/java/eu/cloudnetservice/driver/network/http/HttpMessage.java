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

package eu.cloudnetservice.driver.network.http;

import java.io.InputStream;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents a http message sent from or to the server. The common use case is a http request and response.
 *
 * @param <T> the generic type of the class implementing this interface.
 * @see HttpRequest
 * @see HttpResponse
 * @since 4.0
 */
public interface HttpMessage<T extends HttpMessage<T>> {

  /**
   * Get the context this message is processed in.
   *
   * @return the associated context of this message.
   */
  @NonNull HttpContext context();

  /**
   * Get a header value by the given name.
   *
   * @param name the name of the header to get.
   * @return the header value or null if the header does not exist.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable String header(@NonNull String name);

  /**
   * Get a header value by the given name, converted to an int. Returns null if the header cannot be converted to an int
   * or does not exist.
   *
   * @param name the name of the header to get.
   * @return the header value converted to an int or null.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable Integer headerAsInt(@NonNull String name);

  /**
   * Get the header value by the given name, converted to a bool. If the header does not exist the method defaults to
   * false.
   *
   * @param name the name of the header to get.
   * @return the value converted to a bool or false if the header does not exist.
   * @throws NullPointerException if the given name is null.
   */
  boolean headerAsBoolean(@NonNull String name);

  /**
   * Sets the given header, replacing the current one if already set.
   *
   * @param name  the name of the header.
   * @param value the value of the header.
   * @return the same message as used to call the method, for chaining.
   * @throws NullPointerException if either the given name or value is null.
   */
  @NonNull T header(@NonNull String name, @NonNull String value);

  /**
   * Removes a header by the given name from this message.
   *
   * @param name the name of the header to remove.
   * @return the same message as used to call the method, for chaining.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull T removeHeader(@NonNull String name);

  /**
   * Removes all headers from this message.
   *
   * @return the same message as used to call the method, for chaining.
   */
  @NonNull T clearHeaders();

  /**
   * Checks if this message has a header with the given name set.
   *
   * @param name the name of the header to check.
   * @return true if the header is present, false otherwise.
   * @throws NullPointerException if the given name is null.
   */
  boolean hasHeader(@NonNull String name);

  /**
   * Get all headers of this message converted to a map. The key of the map represents the name of the header, the value
   * of the map represents the value of the header. This map does not allow duplicates.
   *
   * @return all headers of the message collected to a map, duplication free.
   */
  @NonNull Map<String, String> headers();

  /**
   * Get the http version of this http message. CloudNet currently only supports Http 1 and Http 1.1.
   *
   * @return the version of this http message.
   */
  @NonNull HttpVersion version();

  /**
   * Sets the version of this http message.
   *
   * @param version the version to use.
   * @return the same message as used to call the method, for chaining.
   * @throws NullPointerException if the given http version is null.
   */
  @NonNull T version(@NonNull HttpVersion version);

  /**
   * Get the body content of this http message converted to a byte array. If there is no need to read the body fully
   * into the heap, prefer {@link #bodyStream()} instead.
   *
   * @return the body content of this http message.
   */
  byte[] body();

  /**
   * Converts the body of the http message into an utf-8 encoded string.
   *
   * @return the body of the message converted to a string.
   */
  @NonNull String bodyAsString();

  /**
   * Sets the body of the http message. If there is no need to load the full body into the heap, prefer streaming the
   * content by using {@link #body(InputStream)} instead.
   *
   * @param byteArray the body as a byte array.
   * @return the same message as used to call the method, for chaining.
   * @throws UnsupportedOperationException if setting the body is not supported for the http message.
   */
  @NonNull T body(byte[] byteArray);

  /**
   * Sets the body to the given string. This might be useful to e.g. return a json response.
   *
   * @param text the body as a string.
   * @return the same message as used to call the method, for chaining.
   * @throws UnsupportedOperationException if setting the body is not supported for the http message.
   */
  @NonNull T body(@NonNull String text);

  /**
   * Get the body as a streamable content source to reduce the heap load when reading the body content. This method
   * returns null if no http body is provided in the message.
   *
   * @return the body as a content stream, or null if the message has no http body.
   */
  @UnknownNullability InputStream bodyStream();

  /**
   * Sets the body of this http message to the given input stream. Closing of the stream will be done automatically and
   * should not be made after calling the method. Setting the body stream to null will remove the current body.
   *
   * @param body the new body stream to use.
   * @return the same message as used to call the method, for chaining.
   * @throws UnsupportedOperationException if setting the body is not supported for the http message.
   */
  @NonNull T body(@Nullable InputStream body);

  /**
   * Get if the current http message has a body present.
   *
   * @return if a body is present.
   */
  boolean hasBody();
}
