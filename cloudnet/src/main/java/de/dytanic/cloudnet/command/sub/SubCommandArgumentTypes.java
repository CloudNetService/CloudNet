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

package de.dytanic.cloudnet.command.sub;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeCollection;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeDouble;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeEnum;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeHostAndPort;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeInt;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeIntRange;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeServiceTemplate;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeString;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeValidHostAndPort;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class SubCommandArgumentTypes {

  public static QuestionAnswerType<HostAndPort> hostAndPort(String key) {
    return new QuestionAnswerTypeHostAndPort() {
      @Override
      public String getRecommendation() {
        return key;
      }
    };
  }

  public static QuestionAnswerType<HostAndPort> validHostAndPort(String key) {
    return new QuestionAnswerTypeValidHostAndPort() {
      @Override
      public String getRecommendation() {
        return key;
      }
    };
  }

  public static QuestionAnswerType<ServiceTemplate> template(String key) {
    return template(key, false);
  }

  public static QuestionAnswerType<ServiceTemplate> template(String key, boolean existingStorage) {
    return new QuestionAnswerTypeServiceTemplate(existingStorage) {
      @Override
      public String getRecommendation() {
        return key;
      }
    };
  }

  public static QuestionAnswerType<Collection<String>> collection(String key) {
    return collection(null, key);
  }

  public static QuestionAnswerType<Collection<String>> collection(Collection<String> possibleAnswers, String key) {
    return collection(possibleAnswers, key, null);
  }

  public static QuestionAnswerType<Collection<String>> collection(Collection<String> possibleAnswers, String key,
    String invalidMessage) {
    return new QuestionAnswerTypeCollection(possibleAnswers) {
      @Override
      public String getRecommendation() {
        return key;
      }

      @Override
      public String getInvalidInputMessage(@NotNull String input) {
        return invalidMessage != null ? invalidMessage : super.getInvalidInputMessage(input);
      }
    };
  }

  public static <E extends Enum<E>> QuestionAnswerType<E> exactEnum(Class<E> enumClass) {
    return new QuestionAnswerTypeEnum<>(enumClass);
  }

  public static QuestionAnswerType<String> anyStringIgnoreCase(String... allowedValues) {
    return new QuestionAnswerTypeStaticStringArray(allowedValues, true);
  }

  public static QuestionAnswerType<String> anyString(String... allowedValues) {
    return new QuestionAnswerTypeStaticStringArray(allowedValues, false);
  }

  public static QuestionAnswerType<String> exactStringIgnoreCase(String requiredValue) {
    return new QuestionAnswerTypeStaticString(requiredValue, true);
  }

  public static QuestionAnswerType<String> exactString(String requiredValue) {
    return new QuestionAnswerTypeStaticString(requiredValue, false);
  }

  public static QuestionAnswerType<String> url(String key) {
    return dynamicString(key, LanguageManager.getMessage("ca-question-list-invalid-url"), input -> {
      try {
        new URL(input);
        return true;
      } catch (MalformedURLException exception) {
        return false;
      }
    });
  }

  public static QuestionAnswerType<String> dynamicString(String key) {
    return dynamicString(key, null, s -> true);
  }

  public static QuestionAnswerType<String> dynamicString(String key, Predicate<String> tester) {
    return dynamicString(key, null, tester);
  }

  public static QuestionAnswerType<String> dynamicString(String key, int maxLength) {
    return dynamicString(
      key,
      LanguageManager.getMessage("command-sub-argument-string-too-long")
        .replace("%maxLength%", String.valueOf(maxLength)),
      value -> value.length() <= maxLength
    );
  }

  public static QuestionAnswerType<String> dynamicString(String key, String invalidMessage, Predicate<String> tester) {
    return dynamicString(key, invalidMessage, tester, null);
  }

  public static QuestionAnswerType<String> dynamicString(String key,
    Supplier<Collection<String>> possibleAnswersSupplier) {
    return dynamicString(key, null, s -> true, possibleAnswersSupplier);
  }

  public static QuestionAnswerType<String> dynamicString(String key, String invalidMessage, Predicate<String> tester,
    Supplier<Collection<String>> possibleAnswersSupplier) {
    return new QuestionAnswerTypeString() {
      @Override
      public String getRecommendation() {
        return key;
      }

      @Override
      public boolean isValidInput(@NotNull String input) {
        return tester.test(input);
      }

      @Override
      public Collection<String> getPossibleAnswers() {
        return possibleAnswersSupplier != null ? possibleAnswersSupplier.get() : null;
      }

      @Override
      public String getInvalidInputMessage(@NotNull String input) {
        return invalidMessage;
      }
    };
  }

  public static QuestionAnswerType<Double> double_(String key) {
    return new QuestionAnswerTypeDouble() {
      @Override
      public String getRecommendation() {
        return key;
      }
    };
  }

  public static QuestionAnswerType<Double> double_(String key, String invalidMessage) {
    return double_(key, invalidMessage, null);
  }

  public static QuestionAnswerType<Double> double_(String key, String invalidMessage, Predicate<Double> tester) {
    return double_(key, invalidMessage, tester, null);
  }

  public static QuestionAnswerType<Double> double_(String key, String invalidMessage, Predicate<Double> tester,
    Supplier<Collection<Double>> possibleAnswersSupplier) {
    return new QuestionAnswerTypeDouble() {
      @Override
      public String getRecommendation() {
        return key;
      }

      @Override
      public String getInvalidInputMessage(@NotNull String input) {
        return invalidMessage;
      }

      @Override
      public Collection<String> getPossibleAnswers() {
        return possibleAnswersSupplier != null ? possibleAnswersSupplier.get().stream().map(Objects::toString)
          .collect(Collectors.toList()) : null;
      }

      @Override
      public boolean isValidInput(@NotNull String input) {
        return super.isValidInput(input) && tester.test(Double.parseDouble(input));
      }
    };
  }

  public static QuestionAnswerType<Integer> positiveInteger(String key) {
    return integer(
      key,
      LanguageManager.getMessage("command-sub-argument-int-not-positive"),
      value -> value > 0
    );
  }

  public static QuestionAnswerType<Integer> negativeInteger(String key) {
    return integer(
      key,
      LanguageManager.getMessage("command-sub-argument-int-not-negative"),
      value -> value < 0
    );
  }

  public static QuestionAnswerType<Integer> integer(String key, int minValue, int maxValue) {
    return integer(key, null, minValue, maxValue);
  }

  public static QuestionAnswerType<Integer> integer(String key, String invalidMessage, int minValue, int maxValue) {
    return new QuestionAnswerTypeIntRange(minValue, maxValue) {
      @Override
      public String getRecommendation() {
        return key;
      }

      @Override
      public String getInvalidInputMessage(@NotNull String input) {
        return invalidMessage != null ? invalidMessage : super.getInvalidInputMessage(input);
      }
    };
  }

  public static QuestionAnswerType<Boolean> bool(String key) {
    return bool(key, null);
  }

  public static QuestionAnswerType<Boolean> bool(String key, String invalidMessage) {
    return new QuestionAnswerTypeBoolean("true", "false") {
      @Override
      public String getRecommendation() {
        return key;
      }

      @Override
      public String getInvalidInputMessage(@NotNull String input) {
        return invalidMessage != null ? invalidMessage : super.getInvalidInputMessage(input);
      }
    };
  }

  public static QuestionAnswerType<Integer> integer(String key) {
    return new QuestionAnswerTypeInt() {
      @Override
      public String getRecommendation() {
        return key;
      }
    };
  }

  public static QuestionAnswerType<Integer> integer(String key, String invalidMessage) {
    return integer(key, invalidMessage, input -> true);
  }

  public static QuestionAnswerType<Integer> integer(String key, Predicate<Integer> tester) {
    return integer(key, null, tester);
  }

  public static QuestionAnswerType<Integer> integer(String key, String invalidMessage, Predicate<Integer> tester) {
    return integer(key, invalidMessage, tester, null);
  }

  public static QuestionAnswerType<Integer> integer(String key, Predicate<Integer> tester,
    Supplier<Collection<Integer>> possibleAnswersSupplier) {
    return integer(key, null, tester, possibleAnswersSupplier);
  }

  public static QuestionAnswerType<Integer> integer(String key, String invalidMessage, Predicate<Integer> tester,
    Supplier<Collection<Integer>> possibleAnswersSupplier) {
    return new QuestionAnswerTypeInt() {
      @Override
      public String getRecommendation() {
        return key;
      }

      @Override
      public String getInvalidInputMessage(@NotNull String input) {
        return invalidMessage != null ? invalidMessage : super.getInvalidInputMessage(input);
      }

      @Override
      public Collection<String> getPossibleAnswers() {
        return possibleAnswersSupplier != null ? possibleAnswersSupplier.get().stream().map(Objects::toString)
          .collect(Collectors.toList()) : null;
      }

      @Override
      public boolean isValidInput(@NotNull String input) {
        return super.isValidInput(input) && tester.test(Integer.parseInt(input));
      }
    };
  }

}
