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

package de.dytanic.cloudnet.driver.network.http;

public final class HttpResponseCode {

  public static final int
    HTTP_OK = 200,
    HTTP_CREATED = 201,
    HTTP_ACCEPTED = 202,
    HTTP_NOT_AUTHORITATIVE = 203,
    HTTP_NO_CONTENT = 204,
    HTTP_RESET = 205,
    HTTP_PARTIAL = 206,
    HTTP_MULT_CHOICE = 300,
    HTTP_MOVED_PERM = 301,
    HTTP_MOVED_TEMP = 302,
    HTTP_SEE_OTHER = 303,
    HTTP_NOT_MODIFIED = 304,
    HTTP_USE_PROXY = 305,
    HTTP_BAD_REQUEST = 400,
    HTTP_UNAUTHORIZED = 401,
    HTTP_PAYMENT_REQUIRED = 402,
    HTTP_FORBIDDEN = 403,
    HTTP_NOT_FOUND = 404,
    HTTP_BAD_METHOD = 405,
    HTTP_NOT_ACCEPTABLE = 406,
    HTTP_PROXY_AUTH = 407,
    HTTP_CLIENT_TIMEOUT = 408,
    HTTP_CONFLICT = 409,
    HTTP_GONE = 410,
    HTTP_LENGTH_REQUIRED = 411,
    HTTP_PRECON_FAILED = 412,
    HTTP_ENTITY_TOO_LARGE = 413,
    HTTP_REQ_TOO_LONG = 414,
    HTTP_UNSUPPORTED_TYPE = 415,
    HTTP_INTERNAL_ERROR = 500,
    HTTP_NOT_IMPLEMENTED = 501,
    HTTP_BAD_GATEWAY = 502,
    HTTP_UNAVAILABLE = 503,
    HTTP_GATEWAY_TIMEOUT = 504,
    HTTP_VERSION = 505;

  private HttpResponseCode() {
  }

}
