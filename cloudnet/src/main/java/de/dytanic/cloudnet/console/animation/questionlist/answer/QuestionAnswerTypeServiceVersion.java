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

package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionProvider;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class QuestionAnswerTypeServiceVersion implements QuestionAnswerType<Pair<ServiceVersionType, ServiceVersion>> {

  private final Supplier<ServiceEnvironmentType> environmentTypeSupplier;
  private final ServiceVersionProvider serviceVersionProvider;

  public QuestionAnswerTypeServiceVersion(Supplier<ServiceEnvironmentType> environmentTypeSupplier,
    ServiceVersionProvider serviceVersionProvider) {
    this.environmentTypeSupplier = environmentTypeSupplier;
    this.serviceVersionProvider = serviceVersionProvider;
  }

  public QuestionAnswerTypeServiceVersion(ServiceEnvironment environment,
    ServiceVersionProvider serviceVersionProvider) {
    this(() -> Arrays.stream(ServiceEnvironmentType.values())
        .filter(serviceEnvironmentType -> Arrays.asList(serviceEnvironmentType.getEnvironments()).contains(environment))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid ServiceEnvironment")),
      serviceVersionProvider);
  }

  @Override
  public boolean isValidInput(@NotNull String input) {
    if (input.equalsIgnoreCase("none")) {
      return true;
    }
    String[] args = input.split("-");
    if (args.length == 2) {
      Optional<ServiceVersionType> optionalVersionType = this.serviceVersionProvider.getServiceVersionType(args[0]);
      return optionalVersionType.isPresent() &&
        optionalVersionType.get().getTargetEnvironment().getEnvironmentType() == this.environmentTypeSupplier.get() &&
        optionalVersionType.get().getVersion(args[1]).isPresent() &&
        optionalVersionType.get().canInstall(optionalVersionType.get().getVersion(args[1]).get());
    }
    return false;
  }

  @Override
  public Pair<ServiceVersionType, ServiceVersion> parse(@NotNull String input) {
    if (input.equalsIgnoreCase("none")) {
      return null;
    }
    String[] args = input.split("-");
    ServiceVersionType versionType = this.serviceVersionProvider.getServiceVersionType(args[0]).get();
    return new Pair<>(versionType, versionType.getVersion(args[1]).get());
  }

  @Override
  public @NotNull Collection<String> getPossibleAnswers() {
    return this.getCompletableAnswers();
  }

  @Override
  public String getPossibleAnswersAsString() {
    return System.lineSeparator() + String.join(System.lineSeparator(), this.getPossibleAnswers());
  }

  @Override
  public @NotNull List<String> getCompletableAnswers() {
    List<String> completableAnswers = this.serviceVersionProvider.getServiceVersionTypes().values()
      .stream()
      .filter(serviceVersionType -> serviceVersionType.getTargetEnvironment().getEnvironmentType()
        == this.environmentTypeSupplier.get())
      .flatMap(serviceVersionType -> serviceVersionType.getVersions()
        .stream()
        .filter(version -> version.canRun(JavaVersion.getRuntimeVersion()))
        .map(serviceVersion -> serviceVersionType.getName() + "-" + serviceVersion.getName()))
      .collect(Collectors.toList());
    completableAnswers.add("none");
    return completableAnswers;
  }

  @Override
  public String getInvalidInputMessage(@NotNull String input) {
    String[] args = input.split("-");
    if (args.length == 2) {
      Optional<ServiceVersionType> optionalVersionType = this.serviceVersionProvider.getServiceVersionType(args[0]);
      if (optionalVersionType.isPresent()) {
        ServiceVersionType versionType = optionalVersionType.get();
        Optional<ServiceVersion> optionalVersion = versionType.getVersion(args[1]);

        if (optionalVersion.isPresent()) {
          ServiceVersion version = optionalVersion.get();

          if (!versionType.canInstall(version)) {
            return "&c" + LanguageManager.getMessage("command-template-install-wrong-java")
              .replace("%version%", versionType.getName() + "-" + version.getName())
              .replace("%java%", JavaVersion.getRuntimeVersion().getName());
          }
        }
      }
    }
    return LanguageManager.getMessage("ca-question-list-invalid-service-version");
  }

}

