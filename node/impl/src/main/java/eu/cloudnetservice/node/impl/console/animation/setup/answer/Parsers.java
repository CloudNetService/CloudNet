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

package eu.cloudnetservice.node.impl.console.animation.setup.answer;

import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.base.JavaVersion;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.util.JavaVersionResolver;
import eu.cloudnetservice.node.impl.util.NetworkUtil;
import eu.cloudnetservice.node.impl.version.ServiceVersion;
import eu.cloudnetservice.node.impl.version.ServiceVersionProvider;
import eu.cloudnetservice.node.impl.version.ServiceVersionType;
import eu.cloudnetservice.utils.base.StringUtil;
import io.vavr.Tuple2;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.NonNull;

@Singleton
public final class Parsers {

  private final Configuration configuration;
  private final ServiceTaskProvider taskProvider;
  private final ServiceVersionProvider versionProvider;

  @Inject
  public Parsers(
    @NonNull Configuration configuration,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull ServiceVersionProvider versionProvider
  ) {
    this.configuration = configuration;
    this.taskProvider = taskProvider;
    this.versionProvider = versionProvider;
  }

  public @NonNull QuestionAnswerType.Parser<String> nonEmptyStr() {
    return input -> {
      if (input.isEmpty()) {
        throw ParserException.INSTANCE;
      }
      return input;
    };
  }

  public @NonNull QuestionAnswerType.Parser<String> limitedStr(int length) {
    return input -> {
      if (input.length() > length) {
        throw ParserException.INSTANCE;
      }
      return input;
    };
  }

  public @NonNull <T extends Enum<T>> QuestionAnswerType.Parser<T> enumConstant(@NonNull Class<T> enumClass) {
    return input -> Preconditions.checkNotNull(Enums.getIfPresent(enumClass, StringUtil.toUpper(input)).orNull());
  }

  public @NonNull QuestionAnswerType.Parser<String> regex(@NonNull Pattern pattern) {
    return input -> {
      if (pattern.matcher(input).matches()) {
        return input;
      }
      throw ParserException.INSTANCE;
    };
  }

  public @NonNull QuestionAnswerType.Parser<Tuple2<String, JavaVersion>> javaVersion() {
    return input -> {
      var version = JavaVersionResolver.resolveFromJavaExecutable(input);
      if (version == null) {
        throw ParserException.INSTANCE;
      }
      return new Tuple2<>(input.trim(), version);
    };
  }

  public @NonNull QuestionAnswerType.Parser<Tuple2<ServiceVersionType, ServiceVersion>> serviceVersion() {
    return input -> {
      // install no version
      if (input.equalsIgnoreCase("none")) {
        return null;
      }
      // try to split the name of the version
      var result = input.split("-", 2);
      if (result.length != 2) {
        throw ParserException.INSTANCE;
      }
      // get the type
      var type = this.versionProvider.serviceVersionType(result[0]);
      if (type == null) {
        throw ParserException.INSTANCE;
      }

      // get the version
      var version = type.version(result[1]);
      if (version == null) {
        throw ParserException.INSTANCE;
      }

      // combine the result
      return new Tuple2<>(type, version);
    };
  }

  public @NonNull QuestionAnswerType.Parser<ServiceEnvironmentType> serviceEnvironmentType() {
    return input -> {
      var type = this.versionProvider.environmentType(input);
      if (type != null) {
        return type;
      }

      throw ParserException.INSTANCE;
    };
  }

  public @NonNull QuestionAnswerType.Parser<String> nonExistingTask() {
    return input -> {
      var task = this.taskProvider.serviceTask(input);
      if (task != null) {
        throw ParserException.INSTANCE;
      }
      return input.trim();
    };
  }

  @SafeVarargs
  public final @NonNull <T> QuestionAnswerType.Parser<T> allOf(@NonNull QuestionAnswerType.Parser<T>... parsers) {
    return input -> {
      T result = null;
      for (var parser : parsers) {
        result = parser.parse(input);
      }
      return result;
    };
  }

  public @NonNull QuestionAnswerType.Parser<UUID> uuid() {
    return UUID::fromString;
  }

  public @NonNull QuestionAnswerType.Parser<Integer> anyNumber() {
    return Integer::parseInt;
  }

  public @NonNull QuestionAnswerType.Parser<Integer> ranged(int from, int to) {
    return input -> {
      var value = Integer.parseInt(input);
      if (value < from || value > to) {
        throw ParserException.INSTANCE;
      }
      return value;
    };
  }

  public @NonNull QuestionAnswerType.Parser<Boolean> bool() {
    return input -> {
      if (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("no")) {
        return input.equalsIgnoreCase("yes");
      } else {
        throw ParserException.INSTANCE;
      }
    };
  }

  public @NonNull QuestionAnswerType.Parser<HostAndPort> validatedHostAndPort(boolean withPort) {
    return input -> {
      var host = NetworkUtil.parseHostAndPort(input, withPort);
      if (host == null) {
        throw ParserException.INSTANCE;
      }
      return host;
    };
  }

  public @NonNull QuestionAnswerType.Parser<HostAndPort> assignableHostAndPort(boolean withPort) {
    return input -> {
      var host = NetworkUtil.parseAssignableHostAndPort(input, withPort);
      if (host == null) {
        throw ParserException.INSTANCE;
      }
      return host;
    };
  }

  public @NonNull QuestionAnswerType.Parser<HostAndPort> nonWildcardHost(
    @NonNull QuestionAnswerType.Parser<HostAndPort> parser
  ) {
    return input -> {
      var hostAndPort = parser.parse(input);
      if (hostAndPort == null || NetworkUtil.checkWildcard(hostAndPort)) {
        throw ParserException.INSTANCE;
      }

      return hostAndPort;
    };
  }

  public @NonNull QuestionAnswerType.Parser<String> assignableHostAndPortOrAlias() {
    return input -> {
      var ipAlias = this.configuration.ipAliases().get(input);
      // the input is an ip alias
      if (ipAlias != null) {
        return ipAlias;
      }
      // parse a host and check if it is assignable
      var host = NetworkUtil.parseAssignableHostAndPort(input, false);
      // we've found an assignable host
      if (host != null) {
        return host.host();
      }
      throw ParserException.INSTANCE;
    };
  }

  public @NonNull <I, O> QuestionAnswerType.Parser<O> andThen(
    @NonNull QuestionAnswerType.Parser<I> parser,
    @NonNull Function<I, O> combiner
  ) {
    return input -> combiner.apply(parser.parse(input));
  }

  public static final class ParserException extends RuntimeException {

    public static final ParserException INSTANCE = new ParserException();
  }
}
