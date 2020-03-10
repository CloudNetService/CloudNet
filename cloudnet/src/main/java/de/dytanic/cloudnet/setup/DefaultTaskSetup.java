package de.dytanic.cloudnet.setup;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeEnum;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeServiceVersion;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.service.EmptyGroupConfiguration;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class DefaultTaskSetup implements DefaultSetup {

    private static final String GLOBAL_TEMPLATE_PREFIX = "Global";
    private static final String GLOBAL_PROXY_GROUP_NAME = "Global-Proxy";
    private static final String GLOBAL_SERVER_GROUP_NAME = "Global-Server";
    private static final String PROXY_TASK_NAME = "Proxy";
    private static final String LOBBY_TASK_NAME = "Lobby";

    private boolean shouldExecute = false;

    @Override
    public void postExecute(ConsoleQuestionListAnimation animation) {
        if (!this.shouldExecute) {
            return;
        }

        boolean installProxy = (boolean) animation.getResult("installProxy");
        boolean installServer = (boolean) animation.getResult("installServer");

        ServiceEnvironmentType proxyEnvironment = (ServiceEnvironmentType) animation.getResult("proxyEnvironment");
        Pair<ServiceVersionType, ServiceVersion> proxyVersion = (Pair<ServiceVersionType, ServiceVersion>) animation.getResult("proxyVersion");

        ServiceEnvironmentType serverEnvironment = (ServiceEnvironmentType) animation.getResult("serverEnvironment");
        Pair<ServiceVersionType, ServiceVersion> serverVersion = (Pair<ServiceVersionType, ServiceVersion>) animation.getResult("serverVersion");

        GroupConfiguration globalServerGroup = new EmptyGroupConfiguration(GLOBAL_SERVER_GROUP_NAME);
        GroupConfiguration globalProxyGroup = new EmptyGroupConfiguration(GLOBAL_PROXY_GROUP_NAME);

        if (installProxy) {
            this.createDefaultTask(proxyEnvironment, PROXY_TASK_NAME, globalProxyGroup.getName(), 256);
        }

        if (installServer) {
            this.createDefaultTask(serverEnvironment, LOBBY_TASK_NAME, globalServerGroup.getName(), 512);
        }

        if (proxyVersion != null) {
            this.installGlobalTemplate(globalProxyGroup, "proxy", proxyVersion.getFirst(), proxyVersion.getSecond());
        }
        if (serverVersion != null) {
            this.installGlobalTemplate(globalServerGroup, "server", serverVersion.getFirst(), serverVersion.getSecond());
        }

        CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(globalServerGroup);
        CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(globalProxyGroup);
    }

    @Override
    public boolean shouldAsk(boolean configFileAvailable) {
        return !CloudNet.getInstance().getCloudServiceManager().isFileCreated();
    }

    private void installGlobalTemplate(GroupConfiguration globalGroup, String name, ServiceVersionType versionType, ServiceVersion version) {
        ServiceTemplate globalTemplate = new ServiceTemplate(GLOBAL_TEMPLATE_PREFIX, name, "local");
        globalGroup.getTemplates().add(globalTemplate);

        CloudNet.getInstance().getServiceVersionProvider().installServiceVersion(versionType, version, globalTemplate);
    }

    private void createDefaultTask(ServiceEnvironmentType environment, String taskName, String globalGroupName, int maxHeapMemorySize) {
        ServiceTask serviceTask = new ServiceTask(
                new ArrayList<>(),
                new ArrayList<>(Collections.singletonList(new ServiceTemplate(taskName, "default", "local"))),
                new ArrayList<>(),
                taskName,
                "jvm",
                false,
                true,
                false,
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList(taskName, globalGroupName)),
                new ArrayList<>(),
                new ProcessConfiguration(
                        environment,
                        maxHeapMemorySize,
                        new ArrayList<>()
                ),
                environment.getDefaultStartPort(),
                1
        );

        for (ServiceTemplate template : serviceTask.getTemplates()) {
            ITemplateStorage storage = CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, template.getStorage());
            try {
                TemplateStorageUtil.createAndPrepareTemplate(storage, template.getPrefix(), template.getName(), environment);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);
        CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(new EmptyGroupConfiguration(taskName));
    }

    @Override
    public void applyQuestions(ConsoleQuestionListAnimation animation) {
        animation.addEntry(new QuestionListEntry<>(
                "proxyEnvironment",
                LanguageManager.getMessage("cloudnet-init-setup-tasks-proxy-environment"),
                new QuestionAnswerTypeEnum<ServiceEnvironmentType>(ServiceEnvironmentType.class) {
                    @Override
                    protected ServiceEnvironmentType[] values() {
                        return Arrays.stream(super.values()).filter(ServiceEnvironmentType::isMinecraftProxy).toArray(ServiceEnvironmentType[]::new);
                    }
                }
        ));
        animation.addEntry(new QuestionListEntry<>(
                "proxyVersion",
                LanguageManager.getMessage("cloudnet-init-setup-tasks-proxy-version"),
                new QuestionAnswerTypeServiceVersion(() -> (ServiceEnvironmentType) animation.getResult("proxyEnvironment"), CloudNet.getInstance().getServiceVersionProvider())
        ));

        animation.addEntry(new QuestionListEntry<>(
                "serverEnvironment",
                LanguageManager.getMessage("cloudnet-init-setup-tasks-server-environment"),
                new QuestionAnswerTypeEnum<ServiceEnvironmentType>(ServiceEnvironmentType.class) {
                    @Override
                    protected ServiceEnvironmentType[] values() {
                        return Arrays.stream(super.values()).filter(ServiceEnvironmentType::isMinecraftServer).toArray(ServiceEnvironmentType[]::new);
                    }
                }
        ));
        animation.addEntry(new QuestionListEntry<>(
                "serverVersion",
                LanguageManager.getMessage("cloudnet-init-setup-tasks-server-version"),
                new QuestionAnswerTypeServiceVersion(() -> (ServiceEnvironmentType) animation.getResult("serverEnvironment"), CloudNet.getInstance().getServiceVersionProvider())
        ));

        animation.addEntry(new QuestionListEntry<>(
                "installProxy",
                LanguageManager.getMessage("cloudnet-init-setup-tasks-should-install-proxy"),
                new QuestionAnswerTypeBoolean()
        ));
        animation.addEntry(new QuestionListEntry<>(
                "installServer",
                LanguageManager.getMessage("cloudnet-init-setup-tasks-should-install-server"),
                new QuestionAnswerTypeBoolean()
        ));

        this.shouldExecute = true;
    }

}
