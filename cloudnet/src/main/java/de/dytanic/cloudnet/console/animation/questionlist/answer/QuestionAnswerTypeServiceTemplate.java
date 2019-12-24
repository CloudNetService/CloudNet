package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;

import java.util.Collection;
import java.util.stream.Collectors;

public class QuestionAnswerTypeServiceTemplate implements QuestionAnswerType<ServiceTemplate> {
    @Override
    public boolean isValidInput(String input) {
        return parse(input) != null;
    }

    @Override
    public ServiceTemplate parse(String input) {
        return ServiceTemplate.parse(input);
    }

    @Override
    public String getInvalidInputMessage(String input) {
        return LanguageManager.getMessage("ca-question-list-invalid-template");
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE)
                .getTemplates()
                .stream()
                .map(serviceTemplate -> serviceTemplate.getStorage() + ":" + serviceTemplate.getPrefix() + "/" + serviceTemplate.getName())
                .collect(Collectors.toList());
    }
}
