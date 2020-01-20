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

    private boolean existingStorage;

    public QuestionAnswerTypeServiceTemplate(boolean existingStorage) {
        this.existingStorage = existingStorage;
    }

    @Override
    public boolean isValidInput(String input) {
        ServiceTemplate template = parse(input);
        return template != null && (!this.existingStorage || CloudNet.getInstance().getServicesRegistry().containsService(ITemplateStorage.class, template.getStorage()));
    }

    @Override
    public ServiceTemplate parse(String input) {
        return ServiceTemplate.parse(input);
    }

    @Override
    public String getInvalidInputMessage(String input) {
        ServiceTemplate template = this.parse(input);
        return template != null ?
                LanguageManager.getMessage("ca-question-list-invalid-template") :
                LanguageManager.getMessage("ca-question-list-template-invalid-storage");
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
