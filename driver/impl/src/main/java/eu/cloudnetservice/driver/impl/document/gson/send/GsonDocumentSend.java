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

package eu.cloudnetservice.driver.impl.document.gson.send;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.send.DocumentSend;
import eu.cloudnetservice.driver.document.send.element.ArrayElement;
import eu.cloudnetservice.driver.document.send.element.Element;
import eu.cloudnetservice.driver.document.send.element.NullElement;
import eu.cloudnetservice.driver.document.send.element.ObjectElement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

/**
 * An implementation of a document send specifically made for gson.
 *
 * @param rootElement the root object element of the document send.
 * @since 4.0
 */
public record GsonDocumentSend(@NonNull ObjectElement rootElement) implements DocumentSend {

  /**
   * Constructs a document send instance from the given gson json object.
   *
   * @param object the json object to construct the document send from.
   * @return the document send constructed from the given json object.
   * @throws NullPointerException if the given json object is null.
   */
  public static @NonNull DocumentSend fromJsonObject(@NonNull JsonObject object) {
    var serializedObject = serializeObject(Element.NO_KEY, object);
    return new GsonDocumentSend(serializedObject);
  }

  /**
   * Serialises the given gson json object into an object element.
   *
   * @param key    the key of the given json object.
   * @param object the object to serialize.
   * @return an object element serialized from the given gson json object.
   * @throws NullPointerException if the given key or json object is null.
   */
  private static @NonNull ObjectElement serializeObject(@NonNull String key, @NonNull JsonObject object) {
    // construct the target element collection which preserves the initial insertion order
    // copy over the elements from the object to ensure that there are no data races
    Collection<Element> elements = new LinkedList<>();
    Set<Map.Entry<String, JsonElement>> entries = ImmutableSet.copyOf(object.entrySet());

    // travel over all elements in the object and serialize them
    for (var entry : entries) {
      var element = entry.getValue();
      if (element.isJsonNull()) {
        elements.add(new NullElement(entry.getKey()));
      } else if (element.isJsonPrimitive()) {
        var wrappedPrimitive = GsonPrimitiveConverter.unwrapJsonPrimitive(entry.getKey(), element.getAsJsonPrimitive());
        elements.add(wrappedPrimitive);
      } else if (element.isJsonObject()) {
        var wrappedObject = serializeObject(entry.getKey(), element.getAsJsonObject());
        elements.add(wrappedObject);
      } else if (element.isJsonArray()) {
        var wrappedArray = serializeArray(entry.getKey(), element.getAsJsonArray());
        elements.add(wrappedArray);
      } else {
        throw new IllegalArgumentException("Don't known how to handle json element " + element.getClass().getName());
      }
    }

    // final step: make the elements unmodifiable and return the serialized object
    var unmodifiableElements = Collections.unmodifiableCollection(elements);
    return new ObjectElement(key, unmodifiableElements);
  }

  /**
   * Serialises the given gson json array into an array element.
   *
   * @param key   the key of the given json array.
   * @param array the array to serialize.
   * @return an array element serialized from the given gson json array.
   * @throws NullPointerException if the given key or json array is null.
   */
  private static @NonNull ArrayElement serializeArray(@NonNull String key, @NonNull JsonArray array) {
    // construct the target element collection which preserves the initial insertion order
    // copy over the elements from the array to ensure that there are no data races
    Collection<Element> elements = new LinkedList<>();
    Collection<JsonElement> entries = ImmutableList.copyOf(array.asList());

    // travel over all elements in the array and serialize them
    for (var element : entries) {
      if (element.isJsonNull()) {
        elements.add(new NullElement(Element.NO_KEY));
      } else if (element.isJsonPrimitive()) {
        var wrappedPrimitive = GsonPrimitiveConverter.unwrapJsonPrimitive(Element.NO_KEY, element.getAsJsonPrimitive());
        elements.add(wrappedPrimitive);
      } else if (element.isJsonObject()) {
        var wrappedObject = serializeObject(Element.NO_KEY, element.getAsJsonObject());
        elements.add(wrappedObject);
      } else if (element.isJsonArray()) {
        var wrappedArray = serializeArray(Element.NO_KEY, element.getAsJsonArray());
        elements.add(wrappedArray);
      } else {
        throw new IllegalArgumentException("Don't known how to handle json element " + element.getClass().getName());
      }
    }

    // final step: make the elements unmodifiable and return the serialized array
    var unmodifiableElements = Collections.unmodifiableCollection(elements);
    return new ArrayElement(key, unmodifiableElements);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable into(@NonNull DocumentFactory factory) {
    return factory.receive(this);
  }
}
