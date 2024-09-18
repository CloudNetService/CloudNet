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

package eu.cloudnetservice.modules.dns.netcup;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.StandardSerialisationStyle;
import eu.cloudnetservice.modules.dns.util.UnirestToDocumentTransformer;
import io.vavr.control.Try;
import kong.unirest.core.UnirestInstance;
import lombok.NonNull;

final class NetcupSoapRequestSender {

  private final String customerId;
  private final String apiKey;
  private final String apiPassword;

  private final UnirestInstance unirestInstance;

  public NetcupSoapRequestSender(
    @NonNull String customerId,
    @NonNull String apiKey,
    @NonNull String apiPassword,
    @NonNull UnirestInstance unirestInstance
  ) {
    this.customerId = customerId;
    this.apiKey = apiKey;
    this.apiPassword = apiPassword;
    this.unirestInstance = unirestInstance;
  }

  public @NonNull Try<Document> request(@NonNull String action, @NonNull Document params) {
    var requestBodyContent = Document.newJsonDocument().append("action", action).append("param", params);
    var requestBody = requestBodyContent.serializeToString(StandardSerialisationStyle.COMPACT);
    return Try.of(() -> {
      var response = this.unirestInstance.post("")
        .body(requestBody)
        .asObject(UnirestToDocumentTransformer.INSTANCE);
      if (response.isSuccess()) {
        var responseData = response.getBody();
        var statusCode = responseData.getInt("statuscode");
        if (statusCode == 2000) {
          return responseData.readDocument("responsedata");
        } else {
          var message = responseData.getString("longmessage");
          var errorMessage = String.format(
            "Unable to execute action %s on netcup api - server returned status %d (%s)",
            action, statusCode, message);
          throw new IllegalStateException(errorMessage);
        }
      } else {
        var errorMessage = String.format(
          "Unable to execute action %s on netcup api - server returned http status %d (%s)",
          action, response.getStatus(), response.getStatusText());
        throw new IllegalStateException(errorMessage);
      }
    });
  }

  public @NonNull Try<Document> requestAuthenticated(@NonNull String action, @NonNull Document params) {
    return this.requestSessionId().flatMap(sessionId -> {
      var paramsWithAuth = params.mutableCopy()
        .append("apikey", this.apiKey)
        .append("apisessionid", sessionId)
        .append("customernumber", this.customerId);
      return this.request(action, paramsWithAuth);
    });
  }

  private @NonNull Try<String> requestSessionId() {
    var loginParams = Document.newJsonDocument()
      .append("apikey", this.apiKey)
      .append("apipassword", this.apiPassword)
      .append("customernumber", this.customerId);
    return this.request("login", loginParams)
      .map(responseData -> responseData.getString("apisessionid"));
  }
}
