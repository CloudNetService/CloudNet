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

package eu.cloudnetservice.driver.network.netty.http;

import eu.cloudnetservice.driver.network.http.HttpVersion;
import lombok.NonNull;

/**
 * An abstract class which each http message implementation has to override, providing basic access to http version
 * conversion between CloudNet and netty. This class is not a direct subclass of a http message, to prevent the need of
 * a generic type.
 *
 * @since 4.0
 */
public abstract class NettyHttpMessage {

  /**
   * Converts the given netty http version to its CloudNet association.
   *
   * @param httpVersion the netty http version to convert.
   * @return the associated CloudNet http version.
   * @throws NullPointerException if the given netty http version is null.
   */
  protected @NonNull HttpVersion versionFromNetty(@NonNull io.netty5.handler.codec.http.HttpVersion httpVersion) {
    if (httpVersion == io.netty5.handler.codec.http.HttpVersion.HTTP_1_1) {
      return HttpVersion.HTTP_1_1;
    } else {
      return HttpVersion.HTTP_1_0;
    }
  }

  /**
   * Converts the given CloudNet http version to its netty association.
   *
   * @param httpVersion the CloudNet http version to convert.
   * @return the associated netty http version.
   * @throws NullPointerException if the given CloudNet http version is null.
   */
  protected @NonNull io.netty5.handler.codec.http.HttpVersion versionToNetty(@NonNull HttpVersion httpVersion) {
    if (httpVersion == HttpVersion.HTTP_1_1) {
      return io.netty5.handler.codec.http.HttpVersion.HTTP_1_1;
    } else {
      return io.netty5.handler.codec.http.HttpVersion.HTTP_1_0;
    }
  }
}
