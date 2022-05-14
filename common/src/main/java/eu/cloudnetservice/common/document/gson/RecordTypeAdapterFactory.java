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

package eu.cloudnetservice.common.document.gson;

import com.google.common.base.Defaults;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import eu.cloudnetservice.common.collection.Pair;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@Internal
final class RecordTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  public <T> @Nullable TypeAdapter<T> create(@NonNull Gson gson, @NonNull TypeToken<T> type) {
    // don't bother if the type is not a record
    if (!type.getRawType().isRecord()) {
      return null;
    }

    // get the delegate adapter which would be responsible normally
    var delegate = gson.getDelegateAdapter(this, type);
    return new RecordTypeAdapter<>(gson, delegate, type.getRawType());
  }

  private static final class RecordTypeAdapter<T> extends TypeAdapter<T> {

    private final Gson gson;
    private final TypeAdapter<T> delegate;
    private final Class<? super T> originalType;

    // record component name -> type, index
    private final Class<?>[] recordComponentTypes;
    private final Map<String, Pair<TypeToken<?>, Integer>> componentTypes;

    private final Constructor<?> constructor;

    public RecordTypeAdapter(
      @NonNull Gson gson,
      @NonNull TypeAdapter<T> delegate,
      @NonNull Class<? super T> originalType
    ) {
      this.gson = gson;
      this.delegate = delegate;
      this.originalType = originalType;

      var components = originalType.getRecordComponents();
      // init the type component lookup data
      this.recordComponentTypes = new Class[components.length];
      this.componentTypes = new HashMap<>(components.length);
      // fill the data
      for (int i = 0; i < components.length; i++) {
        this.recordComponentTypes[i] = components[i].getType();
        this.componentTypes.put(
          components[i].getName(),
          new Pair<>(TypeToken.get(components[i].getGenericType()), i));
      }

      try {
        this.constructor = originalType.getDeclaredConstructor(this.recordComponentTypes);
        this.constructor.setAccessible(true);
      } catch (NoSuchMethodException exception) {
        throw new IllegalArgumentException(String.format(
          "Unable to resolve record constructor in %s with arguments %s",
          originalType.getName(),
          Arrays.toString(this.recordComponentTypes)
        ), exception);
      }
    }

    @Override
    public void write(@NonNull JsonWriter out, @Nullable T value) throws IOException {
      // no need to think about it
      this.delegate.write(out, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable T read(@NonNull JsonReader in) throws IOException {
      // skip nulls
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      } else {
        // the resulting arguments
        var arguments = new Object[this.componentTypes.size()];

        // read the full object
        in.beginObject();
        while (in.hasNext()) {
          var componentInfo = this.componentTypes.get(in.nextName());
          // the info might be null if we are reading an outdated json entry - skip it then
          if (componentInfo != null) {
            arguments[componentInfo.second()] = this.gson.getAdapter(componentInfo.first()).read(in);
          } else {
            in.skipValue();
          }
        }
        // read finished
        in.endObject();

        // validate the arguments
        for (int i = 0; i < arguments.length; i++) {
          var argumentType = this.recordComponentTypes[i];
          if (argumentType.isPrimitive() && arguments[i] == null) {
            // the entry is missing the json object - use the default one
            arguments[i] = Defaults.defaultValue(argumentType);
          }
        }

        // construct the class
        try {
          return (T) this.constructor.newInstance(arguments);
        } catch (Throwable throwable) {
          throw new IllegalArgumentException(String.format(
            "Unable to create instance of %s with arguments %s",
            this.originalType.getName(),
            Arrays.toString(arguments)
          ), throwable);
        }
      }
    }
  }
}
