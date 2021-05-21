package eu.cloudnetservice.cloudnet.ext.signs.node.util;

import de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.node.configuration.SignConfigurationType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

@ApiStatus.Internal
public final class SignEntryTaskSetup {

    private static final QuestionListEntry<Boolean> CREATE_ENTRY_QUESTION_LIST = new QuestionListEntry<>(
            "GenerateDefaultSignsConfig",
            LanguageManager.getMessage("module-signs-tasks-setup-generate-default-config"),
            SubCommandArgumentTypes.bool("false")
    );

    private SignEntryTaskSetup() {
        throw new UnsupportedOperationException();
    }

    public static void addSetupQuestionIfNecessary(@NotNull ConsoleQuestionListAnimation animation, @NotNull ServiceEnvironmentType type) {
        if (!animation.hasResult("GenerateDefaultSignsConfig") && (type.isMinecraftJavaServer() || type.isMinecraftBedrockServer())) {
            animation.addEntry(CREATE_ENTRY_QUESTION_LIST);
        }
    }

    public static void handleSetupComplete(@NotNull ConsoleQuestionListAnimation animation, @NotNull SignsConfiguration configuration,
                                           @NotNull SignManagement signManagement) {
        if (animation.getName().equals("TaskSetup") && animation.hasResult("GenerateDefaultSignsConfig")) {
            String taskName = (String) animation.getResult("name");
            ServiceEnvironmentType environment = (ServiceEnvironmentType) animation.getResult("environment");
            Boolean generateSignsConfig = (Boolean) animation.getResult("GenerateDefaultSignsConfig");

            if (taskName != null && environment != null && generateSignsConfig != null && generateSignsConfig
                    && !SignPluginInclusion.hasConfigurationEntry(Collections.singleton(taskName), configuration)) {
                SignConfigurationEntry entry = environment.isMinecraftJavaServer()
                        ? SignConfigurationType.JAVA.createEntry(taskName)
                        : SignConfigurationType.BEDROCK.createEntry(taskName);
                configuration.getConfigurationEntries().add(entry);
                signManagement.setSignsConfiguration(configuration);
            }
        }
    }
}
