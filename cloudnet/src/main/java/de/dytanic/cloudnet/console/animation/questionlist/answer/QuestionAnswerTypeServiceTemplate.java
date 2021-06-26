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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class QuestionAnswerTypeServiceTemplate implements QuestionAnswerType<ServiceTemplate> {

  private final boolean existingStorage;

  public QuestionAnswerTypeServiceTemplate(boolean existingStorage) {
    this.existingStorage = existingStorage;
  }

  @Override
  public boolean isValidInput(@NotNull String input) {
    ServiceTemplate template = this.parse(input);
    return template != null && (!this.existingStorage || template.nullableStorage() != null);
  }

  @Override
  public ServiceTemplate parse(@NotNull String input) {
    return ServiceTemplate.parse(input);
  }

  @Override
  public String getInvalidInputMessage(@NotNull String input) {
    ServiceTemplate template = this.parse(input);
    return template == null ?
      LanguageManager.getMessage("ca-question-list-invalid-template") :
      LanguageManager.getMessage("ca-question-list-template-invalid-storage");
  }

  @Override
  public Collection<String> getPossibleAnswers() {
    return this.mapTemplates(CloudNet.getInstance().getLocalTemplateStorage().getTemplates());
  }

  protected Collection<String> mapTemplates(Collection<ServiceTemplate> templates) {
    return templates.stream()
      .map(serviceTemplate -> serviceTemplate.getStorage() + ":" + serviceTemplate.getPrefix() + "/" + serviceTemplate
        .getName())
      .collect(Collectors.toList());
  }
}
