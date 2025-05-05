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

package eu.cloudnetservice.modules.signs.impl.node;

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.impl.node.configuration.SignConfigurationType;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.utils.base.column.ColumnFormatter;
import eu.cloudnetservice.utils.base.column.RowedFormatter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;

@Singleton
@CommandAlias("signs")
@Permission("cloudnet.command.sign")
@Description("module-sign-command-description")
public class SignCommand {

  private static final RowedFormatter<SignConfigurationEntry> ENTRY_LIST_FORMATTER = RowedFormatter.<SignConfigurationEntry>
      builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("targetGroup").build())
    .column(SignConfigurationEntry::targetGroup)
    .build();

  private final SignManagement signManagement;
  private final GroupConfigurationProvider groupProvider;

  @Inject
  public SignCommand(@NonNull SignManagement signManagement, @NonNull GroupConfigurationProvider groupProvider) {
    this.signManagement = signManagement;
    this.groupProvider = groupProvider;
  }

  @Parser(name = "newConfiguration", suggestions = "newConfiguration")
  public @NonNull String newConfigurationParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var name = input.readString();
    var configuration = this.groupProvider.groupConfiguration(name);
    if (configuration == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-general-group-does-not-exist"));
    }

    if (this.signManagement.signsConfiguration().entries()
      .stream()
      .anyMatch(entry -> entry.targetGroup().equalsIgnoreCase(name))) {
      throw new ArgumentNotAvailableException(i18n.translate("module-sign-command-create-entry-group-already-exists"));
    }

    return name;
  }

  @Suggestions("newConfiguration")
  public @NonNull Stream<String> suggestNewConfigurations() {
    return this.groupProvider.groupConfigurations().stream()
      .map(Named::name)
      .filter(group -> this.signManagement.signsConfiguration()
        .entries()
        .stream()
        .noneMatch(entry -> entry.targetGroup().equals(group)));
  }

  @Command("sign|signs list|l")
  public void listConfiguration(@NonNull CommandSource source) {
    source.sendMessage(ENTRY_LIST_FORMATTER.format(this.signManagement.signsConfiguration().entries()));
  }

  @Command("sign|signs create entry <targetGroup>")
  public void createEntry(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument(value = "targetGroup", parserName = "newConfiguration") String targetGroup,
    @Flag("nukkit") boolean nukkit
  ) {
    var entry = nukkit
      ? SignConfigurationType.BEDROCK.createEntry(targetGroup)
      : SignConfigurationType.JAVA.createEntry(targetGroup);
    this.signManagement.signsConfiguration(SignsConfiguration.builder(this.signManagement.signsConfiguration())
      .modifyEntries(entries -> entries.add(entry))
      .build());
    source.sendMessage(i18n.translate("module-sign-command-create-entry-success"));
  }
}
