package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

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
                .map(serviceTemplate -> serviceTemplate.getStorage() + ":" + serviceTemplate.getPrefix() + "/" + serviceTemplate.getName())
                .collect(Collectors.toList());
    }
}
