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

package eu.cloudnetservice.cloudnet.node.console.animation.setup.answer;

import com.google.common.base.Enums;
import com.google.common.base.Verify;
import com.google.common.net.InetAddresses;
import eu.cloudnetservice.cloudnet.common.JavaVersion;
import eu.cloudnetservice.cloudnet.common.collection.Pair;
import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.util.JavaVersionResolver;
import eu.cloudnetservice.cloudnet.node.version.ServiceVersion;
import eu.cloudnetservice.cloudnet.node.version.ServiceVersionType;
import java.net.URI;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.NonNull;

public final class Parsers {

  public Parsers() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull QuestionAnswerType.Parser<String> nonEmptyStr() {
    return input -> {
      if (input.trim().isEmpty()) {
        throw ParserException.INSTANCE;
      }
      return input;
    };
  }

  public static @NonNull <T extends Enum<T>> QuestionAnswerType.Parser<T> enumConstant(@NonNull Class<T> enumClass) {
    return input -> Verify.verifyNotNull(Enums.getIfPresent(enumClass, input.toUpperCase()).orNull());
  }

  public static @NonNull QuestionAnswerType.Parser<String> regex(@NonNull Pattern pattern) {
    return input -> {
      if (pattern.matcher(input).matches()) {
        return input;
      }
      throw ParserException.INSTANCE;
    };
  }

  public static @NonNull QuestionAnswerType.Parser<Pair<String, JavaVersion>> javaVersion() {
    return input -> {
      var version = JavaVersionResolver.resolveFromJavaExecutable(input);
      if (version == null) {
        throw ParserException.INSTANCE;
      }
      return new Pair<>(input.trim(), version);
    };
  }

  public static @NonNull QuestionAnswerType.Parser<Pair<ServiceVersionType, ServiceVersion>> serviceVersion() {
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
      // get the type and version
      var type = CloudNet.instance().serviceVersionProvider()
        .getServiceVersionType(result[0])
        .orElseThrow(() -> ParserException.INSTANCE);
      var version = type.version(result[1]).orElseThrow(() -> ParserException.INSTANCE);
      // combine the result
      return new Pair<>(type, version);
    };
  }

  public static @NonNull QuestionAnswerType.Parser<ServiceEnvironmentType> serviceEnvironmentType() {
    return input -> CloudNet.instance().serviceVersionProvider()
      .getEnvironmentType(input)
      .orElseThrow(() -> ParserException.INSTANCE);
  }

  public static @NonNull QuestionAnswerType.Parser<String> nonExistingTask() {
    return input -> {
      var task = CloudNet.instance().serviceTaskProvider().serviceTask(input);
      if (task != null) {
        throw ParserException.INSTANCE;
      }
      return input.trim();
    };
  }

  public static @NonNull QuestionAnswerType.Parser<UUID> uuid() {
    return UUID::fromString;
  }

  public static @NonNull QuestionAnswerType.Parser<Integer> anyNumber() {
    return Integer::parseInt;
  }

  public static @NonNull QuestionAnswerType.Parser<Integer> ranged(int from, int to) {
    return input -> {
      var value = Integer.parseInt(input);
      if (value < from || value > to) {
        throw ParserException.INSTANCE;
      }
      return value;
    };
  }

  public static @NonNull QuestionAnswerType.Parser<Boolean> bool() {
    return input -> {
      if (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("no")) {
        return input.equalsIgnoreCase("yes");
      } else {
        throw ParserException.INSTANCE;
      }
    };
  }

  public static @NonNull QuestionAnswerType.Parser<HostAndPort> validatedHostAndPort(boolean withPort) {
    return input -> {
      // fetch the uri
      var uri = URI.create("tcp://" + input);
      if (uri.getHost() == null || (withPort && uri.getPort() == -1)) {
        throw ParserException.INSTANCE;
      }
      // check if we can access the address from the uri
      var address = InetAddresses.forUriString(uri.getHost());
      return new HostAndPort(address.getHostAddress(), uri.getPort());
    };
  }

  public static @NonNull <I, O> QuestionAnswerType.Parser<O> andThen(
    @NonNull QuestionAnswerType.Parser<I> parser,
    @NonNull Function<I, O> combiner
  ) {
    return input -> combiner.apply(parser.parse(input));
  }

  public static final class ParserException extends RuntimeException {

    public static final ParserException INSTANCE = new ParserException();
  }
}
