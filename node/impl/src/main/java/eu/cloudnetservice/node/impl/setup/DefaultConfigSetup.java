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

package eu.cloudnetservice.node.impl.setup;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.sun.management.OperatingSystemMXBean;
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.impl.module.DefaultModuleProvider;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.ext.updater.util.ChecksumUtil;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.Node;
import eu.cloudnetservice.node.impl.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.node.impl.console.animation.setup.answer.Parsers;
import eu.cloudnetservice.node.impl.console.animation.setup.answer.QuestionAnswerType;
import eu.cloudnetservice.node.impl.console.animation.setup.answer.QuestionListEntry;
import eu.cloudnetservice.node.impl.module.ModuleEntry;
import eu.cloudnetservice.node.impl.module.ModulesHolder;
import eu.cloudnetservice.node.impl.util.NetworkUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import kong.unirest.core.Unirest;
import lombok.NonNull;

@Singleton
public class DefaultConfigSetup extends DefaultClusterSetup {

  private static final Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings();
  private static final Collection<String> DEFAULT_WHITELIST = ImmutableSet.<String>builder()
    .add("127.0.0.1")
    .add("127.0.1.1")
    .addAll(NetworkUtil.availableIPAddresses())
    .build();
  private static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

  private final I18n i18n;
  private final ModulesHolder modulesHolder;
  private final ModuleProvider moduleProvider;

  @Inject
  public DefaultConfigSetup(
    @NonNull Parsers parsers,
    @NonNull @Service I18n i18n,
    @NonNull ModulesHolder modulesHolder,
    @NonNull Configuration configuration,
    @NonNull ModuleProvider moduleProvider
  ) {
    super(parsers, configuration);
    this.i18n = i18n;
    this.modulesHolder = modulesHolder;
    this.moduleProvider = moduleProvider;
  }

  @Override
  public void applyQuestions(@NonNull ConsoleSetupAnimation animation) {
    // pre-save all available ip addresses
    Collection<String> addresses = NetworkUtil.availableIPAddresses();
    // apply the questions
    animation.addEntries(
      // language
      QuestionListEntry.<Locale>builder()
        .key("language")
        .question(() -> "Welcome to the CloudNet Setup! Please choose the language you want to use")
        .answerType(QuestionAnswerType.<Locale>builder()
          .recommendation(this.i18n.selectedLanguage().toLanguageTag())
          .possibleResults(this.i18n.availableLanguages().stream().map(Locale::toLanguageTag).toList())
          .parser(input -> {
            var locale = Locale.forLanguageTag(input);
            if (this.i18n.availableLanguages().contains(locale)) {
              return locale;
            } else {
              throw Parsers.ParserException.INSTANCE;
            }
          })
          .addResultListener((__, language) -> this.i18n.selectLanguage(language)))
        .build(),
      // eula agreement
      QuestionListEntry.<Boolean>builder()
        .key("eula")
        .translatedQuestion("cloudnet-init-eula")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .recommendation("yes")
          .possibleResults("yes")
          .translatedInvalidInputMessage("cloudnet-init-eula-not-accepted")
          .parser(input -> {
            // we can only proceed if the user accepts the eula
            if (input.equalsIgnoreCase("yes")) {
              return true;
            } else {
              throw Parsers.ParserException.INSTANCE;
            }
          }))
        .build(),
      // network server host
      QuestionListEntry.<HostAndPort>builder()
        .key("internalHost")
        .translatedQuestion("cloudnet-init-setup-internal-host")
        .answerType(QuestionAnswerType.<HostAndPort>builder()
          .recommendation(NetworkUtil.localAddress() + ":1410")
          .possibleResults(addresses.stream().map(addr -> addr + ":1410").toList())
          .parser(this.parsers.assignableHostAndPort(true)))
        .build(),
      // web server host
      QuestionListEntry.<HostAndPort>builder()
        .key("webHost")
        .translatedQuestion("cloudnet-init-setup-web-host")
        .answerType(QuestionAnswerType.<HostAndPort>builder()
          .recommendation(NetworkUtil.localAddress() + ":2812")
          .possibleResults(addresses.stream().map(addr -> addr + ":2812").toList())
          .parser(this.parsers.assignableHostAndPort(true)))
        .build(),
      // service bind host address
      QuestionListEntry.<HostAndPort>builder()
        .key("hostAddress")
        .translatedQuestion("cloudnet-init-setup-host-address")
        .answerType(QuestionAnswerType.<HostAndPort>builder()
          .possibleResults(addresses)
          .recommendation(NetworkUtil.localAddress())
          .parser(this.parsers.nonWildcardHost(this.parsers.assignableHostAndPort(false))))
        .build(),
      // maximum memory usage
      QuestionListEntry.<Integer>builder()
        .key("memory")
        .translatedQuestion("cloudnet-init-setup-memory")
        .answerType(QuestionAnswerType.<Integer>builder()
          .parser(this.parsers.ranged(128, Integer.MAX_VALUE))
          .possibleResults("128", "512", "1024", "2048", "4096", "8192", "16384", "32768", "65536")
          .recommendation((int) ((OS_BEAN.getTotalMemorySize() / (1024 * 1024)) - 512)))
        .build(),
      // default installed modules
      QuestionListEntry.<Collection<ModuleEntry>>builder()
        .key("initialModules")
        .translatedQuestion("cloudnet-init-default-modules")
        .answerType(QuestionAnswerType.<Collection<ModuleEntry>>builder()
          .parser(string -> {
            var entries = SPACE_SPLITTER.splitToList(string);
            if (entries.isEmpty()) {
              return Set.of();
            }
            // try to find each provided module
            Set<ModuleEntry> result = new HashSet<>();
            for (var entry : entries) {
              // get the associated entry
              var moduleEntry = this.modulesHolder
                .findByName(entry)
                .orElseThrow(() -> Parsers.ParserException.INSTANCE);
              // check for depending on modules
              if (!moduleEntry.dependingModules().isEmpty()) {
                moduleEntry.dependingModules().forEach(module -> {
                  // resolve and add the depending on module
                  var dependEntry = this.modulesHolder
                    .findByName(module)
                    .orElseThrow(() -> Parsers.ParserException.INSTANCE);
                  result.add(dependEntry);
                });
              }
              // register the module as installation target
              result.add(moduleEntry);
            }
            return result;
          })
          .possibleResults(this.modulesHolder.entries().stream()
            .map(ModuleEntry::name)
            .toList())
          .recommendation("CloudNet-Bridge CloudNet-Signs")
          .addResultListener((__, result) -> {
            // install all the modules
            result.forEach(entry -> {
              // ensure that the target path actually is there
              var targetPath = DefaultModuleProvider.DEFAULT_MODULE_DIR.resolve(entry.name() + ".jar");
              FileUtil.createDirectory(targetPath.getParent());
              // download the module file
              Unirest.get(entry.url()).asFile(targetPath.toString(), StandardCopyOption.REPLACE_EXISTING);
              // validate the downloaded file
              var checksum = ChecksumUtil.fileShaSum(targetPath);
              if (!checksum.equals(entry.sha3256()) && !Node.DEV_MODE && !entry.official()) {
                // remove the file
                FileUtil.delete(targetPath);
                return;
              }
              // load the module
              this.moduleProvider.loadModule(targetPath);
            });
          }))
        .build()
    );
    // apply the cluster setup questions
    super.applyQuestions(animation);
  }

  @Override
  public void handleResults(@NonNull ConsoleSetupAnimation animation) {
    // language
    this.configuration.language(animation.result("language"));

    // init the local node identity
    HostAndPort host = animation.result("internalHost");
    this.configuration.identity(new NetworkClusterNode(
      animation.hasResult("nodeId") ? animation.result("nodeId") : "Node-1",
      Lists.newArrayList(host)));
    // whitelist the host address
    this.configuration.ipWhitelist().add(host.host());

    // init the host address
    HostAndPort hostAddress = animation.result("hostAddress");
    this.configuration.hostAddress(hostAddress.host());

    // set the maximum memory
    this.configuration.maxMemory(animation.result("memory"));

    // whitelist all default addresses and finish by saving the config
    this.configuration.ipWhitelist().addAll(DEFAULT_WHITELIST);
    this.configuration.save();

    // apply animation results of the cluster setup
    super.handleResults(animation);
  }
}
