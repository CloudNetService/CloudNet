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

package eu.cloudnetservice.modules.dns.util;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import java.util.function.Function;
import kong.unirest.core.RawResponse;
import lombok.NonNull;

public final class UnirestToDocumentTransformer implements Function<RawResponse, Document> {

  public static final UnirestToDocumentTransformer INSTANCE = new UnirestToDocumentTransformer();

  private UnirestToDocumentTransformer() {
  }

  @Override
  public @NonNull Document apply(@NonNull RawResponse rawResponse) {
    var responseBody = rawResponse.getContentAsString();
    return responseBody.isBlank() ? Document.emptyDocument() : DocumentFactory.json().parse(responseBody);
  }
}
