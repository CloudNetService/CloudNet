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

package eu.cloudnetservice.driver.document.property;

import eu.cloudnetservice.driver.document.Document;
import java.lang.reflect.Type;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a property which can be written and read from a {@link Document} or {@link DocPropertyHolder}, for example
 * the amount of online players, the state of the service or other custom properties appended by a plugin. This
 * simplifies accessing these properties by making a common wrapper once.
 * <p>
 * The old code might look like this:
 * <pre>
 * {@code
 *  public final class GameListener implement Listener {
 *    public void handleJoin(PlayerLoginEvent event) {
 *      ServiceInfoSnapshot snapshot = this.manager.currentSnapshot();
 *      GameState state = snapshot.propertyHolder().get("state", GameState.class);
 *
 *      if (state == GameState.STARTED) {
 *        event.setResult(PlayerLoginEvent.Result.DENIED);
 *      }
 *    }
 *  }
 * }
 * </pre>
 * <p>
 * All the property read code can now be replaced by a simple static field like this:
 * <pre>
 * {@code
 *  public final class GameListener implement Listener {
 *    public static final DocProperty<GameState> STATE =
 *      DocProperty.property("state", GameState.class);
 *
 *    public void handleJoin(PlayerLoginEvent event) {
 *      // a service snapshot is a property holder
 *      DocPropertyHolder propertyHolder = this.manager.currentSnapshot();
 *      GameState state = propertyHolder.readProperty(STATE);
 *
 *      if (state == GameState.STARTED) {
 *        event.setResult(PlayerLoginEvent.Result.DENIED);
 *      }
 *    }
 *  }
 * }
 * </pre>
 *
 * @param <E> the type which gets read/written by this property.
 * @since 4.0
 */
public interface DocProperty<E> {

  /**
   * Creates a new property that reads and writes values of the given type from/into a document using the given key.
   *
   * @param key  the key that is used when the member is written to/read from the underlying document.
   * @param type the type of the member that gets wrapped by the property.
   * @param <E>  the type of member that is processed by the doc property.
   * @return a doc property that can read/write members with the given key and type from a document.
   * @throws NullPointerException if the given key or type is null.
   */
  static @NonNull <E> DocProperty<E> property(@NonNull String key, @NonNull Class<E> type) {
    return new StandardDocProperty<>(key, type);
  }

  /**
   * Creates a new property that reads and writes values of the given type from/into a document using the given key.
   *
   * @param key  the key that is used when the member is written to/read from the underlying document.
   * @param type the type of the member that gets wrapped by the property.
   * @param <E>  the type of member that is processed by the doc property.
   * @return a doc property that can read/write members with the given key and type from a document.
   * @throws NullPointerException if the given key or type is null.
   */
  static @NonNull <E> DocProperty<E> genericProperty(@NonNull String key, @NonNull Type type) {
    return new StandardDocProperty<>(key, type);
  }

  /**
   * Get the key of the member that is read from/written to a document.
   *
   * @return the key of the member that is read from/written to a document.
   */
  @NonNull String key();

  /**
   * Get if this property only supports being read from a document but not being written to one.
   *
   * @return true if this property is read-only, false otherwise.
   */
  boolean readOnly();

  /**
   * Wraps this property in a new property that makes any write operation with this property throw an exception. Read
   * operations will be delegated to the property this method is being called on.
   * <p>
   * If this property is already read-only the current property is returned without wrapping.
   *
   * @return a property that wraps this property to make it read-only.
   */
  @CheckReturnValue
  @NonNull DocProperty<E> asReadOnly();

  /**
   * Wraps this property to make read and write operations set a default value in case the given value is either absent
   * (not appended to the document) or set to {@code null}. Read and write operations are delegated to the property this
   * method is being called on.
   * <p>
   * If this property already has a default and the given default is the same as current one (as defined by {@code ==})
   * the current property is returned without wrapping.
   *
   * @param def the default value to use for the property.
   * @return a property that wraps this property and sets a default value during read and write operations.
   */
  @CheckReturnValue
  @NonNull DocProperty<E> withDefault(@Nullable E def);

  /**
   * Wraps this property to apply a rewrite function when reading the property from a document. The parsed type of this
   * property is passed to the function and returned as the new property value. Null values are not passed to the
   * function and returned without processing.
   * <p>
   * Note: as this method only sets a function to rewrite while reading the value, the returned property will be
   * read-only as values passed for writing cannot be converted. Use {@link #withReadWriteRewrite(Function, Function)}
   * to set a function that is called during writing as well.
   *
   * @param rewriteFunction the rewrite function to call when reading the value from a document.
   * @param <V>             the type of value that is the result of the value convert process.
   * @return a read-only property that wraps this property and applies the given rewrite function when reading.
   * @throws NullPointerException if the given rewrite function is null.
   */
  @CheckReturnValue
  @NonNull <V> DocProperty<V> withReadRewrite(@NonNull Function<E, V> rewriteFunction);

  /**
   * Wraps this property to apply a rewrite function when reading and writing the property from/to a document. The
   * parsed type of this property is passed to the function and returned/written as the new property value. Null values
   * are not passed to the function and returned without processing.
   * <p>
   * Note: if the call to this method only sets a function to rewrite while reading the value (setting the rewrite
   * function for reads to {@code null}), the returned property will be read-only as values passed for writing cannot be
   * converted.
   *
   * @param readRewriteFunction  the rewrite function to call when reading the value from a document.
   * @param writeRewriteFunction the rewrite function to call when writing the value to a document, can be null.
   * @param <V>                  the type of value that is the result of the value convert process.
   * @return a property that wraps this property and applies the given rewrite function when reading or writing.
   * @throws NullPointerException if the given read rewrite function is null.
   */
  @CheckReturnValue
  @NonNull <V> DocProperty<V> withReadWriteRewrite(
    @NonNull Function<E, V> readRewriteFunction,
    @Nullable Function<V, E> writeRewriteFunction);

  /**
   * Called to read the value of this property from the given document.
   *
   * @param document the document to read the value from.
   * @return the parsed value from the document, or null if the value is not present.
   * @throws NullPointerException if the given document is null.
   */
  @Nullable E readFrom(@NonNull Document document);

  /**
   * Writes the given value into the given document.
   *
   * @param document the document to write the value to.
   * @param value    the value to write.
   * @return the same property as used to call the method, for chaining.
   * @throws NullPointerException          if the given document is null.
   * @throws UnsupportedOperationException if this property does not support writing values to a document.
   */
  @NonNull DocProperty<E> writeTo(@NonNull Document.Mutable document, @Nullable E value);
}
