package de.dytanic.cloudnet.common.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.TypeAdapters;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.gson.JsonDocumentTypeAdapter;

/**
 * Includes a Gson object as member, which should protect for multiple creation
 * of Gson instances
 *
 * @see Gson
 */
public final class GsonUtil {

  private GsonUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * The Gson constant instance, which should use as a new Gson object instance
   * The following attributes has with GsonBuilder.
   *
   * The serializer has no pretty printing. You can use new
   * JsonDocument(obj).toPrettyJson(); als alternative
   *
   * @see Gson
   * @see GsonBuilder
   * <p>
   * serializeNulls disableHtmlEscaping
   */
  public static final Gson GSON = new GsonBuilder()
      .serializeNulls()
      .disableHtmlEscaping()
      .registerTypeAdapterFactory(TypeAdapters
          .newTypeHierarchyFactory(JsonDocument.class,
              new JsonDocumentTypeAdapter()))
      .create();
}