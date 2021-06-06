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

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents a sub command of any command.
 * <p>
 * If neither minArgs nor maxArgs is set
 */
public abstract class SubCommand implements SubCommandExecutor {

  private final QuestionAnswerType<?>[] requiredArguments;
  private String permission;
  private String description;
  private String extendedUsage = "";
  private boolean onlyConsole = false;
  private boolean disableMinArgsIndicator = false;
  private boolean propertiesEnabled = false;
  private boolean async = false;
  private int minArgs = -1;
  private int exactArgs = -1;
  private int maxArgs = -1;

  public SubCommand(int exactArgs, QuestionAnswerType<?>[] requiredArguments) {
    this.exactArgs = exactArgs;
    this.requiredArguments = requiredArguments;
  }

  public SubCommand(int minArgs, int exactArgs, int maxArgs, QuestionAnswerType<?>[] requiredArguments) {
    this.minArgs = minArgs;
    this.exactArgs = exactArgs;
    this.maxArgs = maxArgs;
    this.requiredArguments = requiredArguments;
  }

  public SubCommand(QuestionAnswerType<?>... requiredArguments) {
    this.requiredArguments = requiredArguments;
  }

  /**
   * Disallows any execution of this command that is not from a {@link de.dytanic.cloudnet.command.ConsoleCommandSender}.
   *
   * @return {@code this}
   */
  public SubCommand onlyConsole() {
    this.onlyConsole = true;
    return this;
  }

  /**
   * Enables that the {@link #execute(SubCommand, ICommandSender, String, SubCommandArgumentWrapper, String, Properties,
   * Map)} method will be called asynchronously to the console thread
   *
   * @return {@code this}
   */
  public SubCommand async() {
    this.async = true;
    return this;
  }

  /**
   * Disables the "..." in the {@link #getArgsAsString()} method
   *
   * @return {@code this}
   */
  public SubCommand disableMinArgsIndicator() {
    this.disableMinArgsIndicator = true;
    return this;
  }

  /**
   * Allows extra properties for this sub command
   *
   * @return {@code this}
   */
  public SubCommand enableProperties() {
    this.propertiesEnabled = true;
    return this;
  }

  /**
   * Appends the given usage to the usage of this command with a space before the new usage.
   *
   * @param usage the usage to be appended
   */
  public void appendUsage(String usage) {
    this.extendedUsage += " " + usage;
  }

  /**
   * Checks whether this command requires the exact length of input arguments or not.
   *
   * @return {@code true} if this command requires it or {@code false} if not
   */
  public boolean requiresExactArgs() {
    return this.exactArgs != -1 || (this.minArgs == -1 && this.maxArgs == -1);
  }

  /**
   * Checks whether this command requires a minimal length of input argument.
   *
   * @return {@code true} if this command requires it or {@code false} if not
   */
  public boolean requiresMinArgs() {
    return this.minArgs != -1;
  }

  public boolean mayLastArgContainSpaces() {
    return this.requiresMinArgs() && this.minArgs == this.requiredArguments.length;
  }

  /**
   * Checks whether this command requires a maximum length of input arguments.
   *
   * @return {@code true} if this command requires it or {@code false} if not
   */
  public boolean requiresMaxArgs() {
    return this.maxArgs != -1;
  }

  /**
   * Checks whether the input length of arguments is valid according to the exact, min and max length of arguments
   * specified for this command.
   *
   * @param length the input arguments length
   * @return {@code true} if the length matches or {@code false} if not
   * @see #requiresExactArgs()
   * @see #requiresMinArgs()
   * @see #requiresMaxArgs()
   */
  public boolean checkValidArgsLength(int length) {
    if (this.requiresMinArgs() && length < this.minArgs) {
      return false;
    }
    if (this.propertiesEnabled && length >= (this.requiresExactArgs() ? this.exactArgs == -1
      ? this.requiredArguments.length : this.exactArgs : this.minArgs)) {
      return true;
    }
    return
      (!this.requiresExactArgs() || (this.exactArgs == -1 ? this.requiredArguments.length : this.exactArgs) == length)
        && (!this.requiresMaxArgs() || length <= this.maxArgs);
  }

  //the returned pair contains the message of the first non-matching argument and the amount of non-matching, static arguments
  public Pair<String, Integer> getInvalidArgumentMessage(String[] args) {
    String resultMessage = null;
    int nonMatched = 0;
    for (int i = 0; i < args.length; i++) {
      if (this.propertiesEnabled && i >= this.getBeginOfProperties()) {
        break;
      }

      if (this.requiredArguments.length > i) {

        QuestionAnswerType<?> type = this.requiredArguments[i];

        if (!type.isValidInput(args[i])) {
          if (type instanceof QuestionAnswerTypeStaticString || type instanceof QuestionAnswerTypeStaticStringArray) {
            ++nonMatched;
          }

          String invalidMessage = type.getInvalidInputMessage(args[i]);
          if (invalidMessage != null && resultMessage == null) {
            resultMessage = invalidMessage;
          }
        }

      } else {

        String currentValue = String.join(" ", Arrays.copyOfRange(args, Math.max(0, i - 1),
          Math.max(this.requiredArguments.length, Math.min(args.length, this.maxArgs))));
        QuestionAnswerType<?> type = this.requiredArguments[this.requiredArguments.length - 1];

        if (!type.isValidInput(currentValue)) {
          if (type instanceof QuestionAnswerTypeStaticString || type instanceof QuestionAnswerTypeStaticStringArray) {
            ++nonMatched;
          }

          String invalidMessage = type.getInvalidInputMessage(currentValue);
          if (invalidMessage != null && resultMessage == null) {
            resultMessage = invalidMessage;
          }
        }

        break;

      }
    }
    return resultMessage != null ? new Pair<>(resultMessage, nonMatched) : null;
  }

  public SubCommandArgument<?>[] parseArgs(String[] args) {
    if (!this.checkValidArgsLength(args.length)) {
      return null;
    }
    return this.parseArgsIgnoreLength(args);
  }

  public SubCommandArgument<?>[] parseArgsIgnoreLength(String[] args) {
    List<SubCommandArgument<?>> result = new ArrayList<>();

    for (int i = 0; i < args.length; i++) {
      if (this.propertiesEnabled && i >= this.getBeginOfProperties()) {
        break;
      }
      if (this.requiredArguments.length > i) {
        QuestionAnswerType<?> type = this.requiredArguments[i];
        if (!type.isValidInput(args[i])) {
          return null;
        }

        result.add(new SubCommandArgument(type, type.parse(args[i])));
      } else {
        String currentValue = String.join(" ", Arrays.copyOfRange(args, Math.max(0, i - 1),
          Math.max(this.requiredArguments.length, Math.min(args.length, this.maxArgs))));
        QuestionAnswerType<?> type = this.requiredArguments[this.requiredArguments.length - 1];

        if (type.isValidInput(currentValue)) {
          result.set(result.size() - 1, new SubCommandArgument(type, type.parse(currentValue)));
        } else {
          return null;
        }

        break;
      }
    }
    return result.toArray(new SubCommandArgument[0]);
  }

  public Properties parseProperties(String[] args) {
    if (!this.propertiesEnabled) {
      return null;
    }
    if (this.getBeginOfProperties() > args.length) {
      return new Properties();
    }
    return Properties.parseLine(Arrays.copyOfRange(args, this.getBeginOfProperties(), args.length));
  }

  private int getBeginOfProperties() {
    return (this.requiresMaxArgs() ? this.maxArgs : this.minArgs == -1 ? this.requiredArguments.length : this.minArgs);
  }

  /**
   * Gets the possible answers for the next argument by the given args array for auto completion. All arguments in the
   * array have to match to get any response.
   *
   * @param args the current args to get the next argument from
   * @return a list containing all possible answers or null, if no argument was found or the next argument doesn't have
   * any answers defined
   */
  public Collection<String> getNextPossibleArgumentAnswers(String[] args) {
    for (int i = 0; i < args.length - 1; i++) {
      if (this.requiredArguments.length > i) {
        if (!this.requiredArguments[i].isValidInput(args[i])) {
          return null;
        }
      }
    }
    if (this.requiredArguments.length >= args.length) {
      return this.requiredArguments[args.length - 1].getPossibleAnswers();
    }
    return null;
  }

  /**
   * Gets all arguments of this sub command as a string.<br> Static strings ({@link
   * SubCommandArgumentTypes#exactString(String)} {@link SubCommandArgumentTypes#anyString(String...)} or the ignore
   * case methods) are never wrapped with brackets.<br>
   * <br>
   * Dynamic strings (any other {@link QuestionAnswerType}) are wrapped with "&lt;&gt;" if they are required and with
   * "[]" if they are optional (this can be set with the {@link #setMinArgs(int)} and {@link #setMaxArgs(int)}
   * methods).
   *
   * @return the arguments as a string split by spaces (the argument
   */
  public String getArgsAsString() {
    Collection<String> args = new ArrayList<>();
    int i = 0;
    for (QuestionAnswerType<?> requiredArgument : this.requiredArguments) {

      String recommendation = requiredArgument.getRecommendation();
      Collection<String> possibleAnswers = requiredArgument.getPossibleAnswers();

      boolean required = this.requiresExactArgs() || (this.requiresMinArgs() && i + 1 <= this.minArgs);
      String answer;
      if (possibleAnswers == null || possibleAnswers.isEmpty() || recommendation != null) {

        answer = recommendation;
        answer = required ? ("<" + answer + ">") : ("[" + answer + "]");

      } else {
        if (possibleAnswers.size() == 1) {
          answer = possibleAnswers.iterator().next();
        } else {
          answer = String.join(", ", possibleAnswers);
          answer = required ? ("<" + answer + ">") : ("[" + answer + "]");
        }
      }

      if (!this.disableMinArgsIndicator && this.requiresMinArgs() && i == this.minArgs - 1
        && this.minArgs == this.requiredArguments.length) {
        answer += " ...";
      }

      args.add(answer);
      ++i;
    }
    return String.join(" ", args);
  }

  public boolean isOnlyConsole() {
    return this.onlyConsole;
  }

  public boolean isAsync() {
    return this.async;
  }

  public String getPermission() {
    return this.permission;
  }

  public String getDescription() {
    return this.description;
  }

  public String getExtendedUsage() {
    return this.extendedUsage;
  }

  public int getMinArgs() {
    return this.minArgs;
  }

  public SubCommand setMinArgs(int minArgs) {
    this.minArgs = minArgs;
    return this;
  }

  public int getExactArgs() {
    return this.exactArgs;
  }

  public SubCommand setExactArgs(int exactArgs) {
    this.exactArgs = exactArgs;
    return this;
  }

  public int getMaxArgs() {
    return this.maxArgs;
  }

  public SubCommand setMaxArgs(int maxArgs) {
    this.maxArgs = maxArgs;
    return this;
  }

  public QuestionAnswerType<?>[] getRequiredArguments() {
    return this.requiredArguments;
  }

}
