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

package eu.cloudnetservice.node.http.annotation;

import eu.cloudnetservice.driver.network.http.HttpContextPreprocessor;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationParser;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.node.config.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Method;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class HeaderAnnotationExtension {

  private final Configuration config;

  @Inject
  public HeaderAnnotationExtension(@NonNull Configuration config) {
    this.config = config;
  }

  public void install(@NonNull HttpAnnotationParser<?> annotationParser) {
    annotationParser
      .registerAnnotationProcessor(new ApplyHeadersProcessor());
  }

  private final class ApplyHeadersProcessor implements HttpAnnotationProcessor {

    @Override
    public @Nullable HttpContextPreprocessor buildPreprocessor(@NonNull Method method, @NonNull Object handlerInstance) {
      if (method.getDeclaringClass().isAnnotationPresent(ApplyHeaders.class) || method.isAnnotationPresent(ApplyHeaders.class)) {
        return (path, ctx) -> {
          ctx.response().header("Access-Control-Allow-Origin",
            HeaderAnnotationExtension.this.config.restConfiguration().cors().allowedOrigins());
          ctx.response().header("Access-Control-Allow-Headers",
            HeaderAnnotationExtension.this.config.restConfiguration().cors().allowedHeaders());
          ctx.response().header("Access-Control-Expose-Headers",
            HeaderAnnotationExtension.this.config.restConfiguration().cors().exposedHeaders());
          ctx.response().header("Access-Control-Allow-Methods",
            HeaderAnnotationExtension.this.config.restConfiguration().cors().allowedMethods());
          ctx.response().header("Access-Control-Allow-Credentials",
            String.valueOf(HeaderAnnotationExtension.this.config.restConfiguration().cors().allowCredentials()));
          ctx.response().header("Access-Control-Max-Age",
            String.valueOf(HeaderAnnotationExtension.this.config.restConfiguration().cors().maxAge()));

          HeaderAnnotationExtension.this.config.restConfiguration().headers().forEach(ctx.response()::header);
          return ctx;
        };
      }
      return null;
    }

  }

}
