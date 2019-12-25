package de.dytanic.cloudnet.command.sub;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeStaticString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class SubCommand implements SubCommandExecutor {

    private String permission;
    private String description;
    private String extendedUsage = "";

    private boolean onlyConsole = false;

    private int minArgs = -1;
    private int exactArgs = -1;
    private int maxArgs = -1;
    private QuestionAnswerType<?>[] requiredArguments;

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

    private void validateArguments() {
        Validate.assertFalse(this.requiredArguments.length == 0, "Min 1 unique argument required");
    }

    public SubCommand onlyConsole() {
        this.onlyConsole = true;
        return this;
    }

    public void expandUsage(String usage) {
        this.extendedUsage += " " + usage;
    }

    public boolean requiresExactArgs() {
        return this.exactArgs != -1 || (this.minArgs == -1 && this.maxArgs == -1);
    }

    public boolean requiresMinArgs() {
        return this.minArgs != -1;
    }

    public boolean requiresMaxArgs() {
        return this.maxArgs != -1;
    }

    public SubCommand setMinArgs(int minArgs) {
        this.minArgs = minArgs;
        return this;
    }

    public SubCommand setMaxArgs(int maxArgs) {
        this.maxArgs = maxArgs;
        return this;
    }

    public SubCommand setExactArgs(int exactArgs) {
        this.exactArgs = exactArgs;
        return this;
    }

    public boolean checkValidArgsLength(int length) {
        return (!this.requiresExactArgs() || (this.exactArgs == -1 ? this.requiredArguments.length : this.exactArgs) == length) &&
                (!this.requiresMinArgs() || length >= this.minArgs) &&
                (!this.requiresMaxArgs() || length <= this.maxArgs);
    }

    //the returned pair contains the message of the first non-matching argument and the amount of non-matching, static arguments
    public Pair<String, Integer> getInvalidArgumentMessage(String[] args) {
        String resultMessage = null;
        int nonMatched = 0;
        for (int i = 0; i < args.length; i++) {
            if (this.requiredArguments.length > i) {

                QuestionAnswerType<?> type = this.requiredArguments[i];

                if (!type.isValidInput(args[i])) {
                    if (type instanceof QuestionAnswerTypeStaticString) {
                        ++nonMatched;
                    }

                    String invalidMessage = type.getInvalidInputMessage(args[i]);
                    if (invalidMessage != null && resultMessage == null) {
                        resultMessage = invalidMessage;
                    }
                }

            } else if (maxArgs >= 0) {

                String currentValue = String.join(" ", Arrays.copyOfRange(args, Math.max(0, i - 1), Math.min(this.requiredArguments.length, Math.min(args.length, this.maxArgs))));
                QuestionAnswerType<?> type = this.requiredArguments[this.requiredArguments.length - 1];

                if (!type.isValidInput(currentValue)) {
                    if (type instanceof QuestionAnswerTypeStaticString) {
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

    public Object[] parseArgs(String[] args) {
        if (!this.checkValidArgsLength(args.length)) {
            return null;
        }
        return this.parseArgsIgnoreLength(args);
    }

    public Object[] parseArgsIgnoreLength(String[] args) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (this.requiredArguments.length > i) {
                if (!this.requiredArguments[i].isValidInput(args[i])) {
                    return null;
                }

                result.add(this.requiredArguments[i].parse(args[i]));
            } else {
                String currentValue = String.join(" ", Arrays.copyOfRange(args, Math.max(0, i - 1), Math.max(this.requiredArguments.length, Math.min(args.length, this.maxArgs))));
                QuestionAnswerType<?> type = this.requiredArguments[this.requiredArguments.length - 1];

                if (type.isValidInput(currentValue)) {
                    result.set(result.size() - 1, type.parse(currentValue));
                }

                break;
            }
        }
        return result.toArray(new Object[0]);
    }

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

    public String getRequiredArgsAsString() {
        Collection<String> args = new ArrayList<>();
        int i = 0;
        for (QuestionAnswerType<?> requiredArgument : this.requiredArguments) {

            String recommendation = requiredArgument.getRecommendation();
            Collection<String> possibleAnswers = requiredArgument.getPossibleAnswers();

            boolean required = this.requiresExactArgs() || (this.requiresMinArgs() && i <= this.minArgs);
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

                if (this.requiresMinArgs() && i == this.minArgs) {
                    answer += " ...";
                }
            }

            args.add(answer);
            ++i;
        }
        return String.join(" ", args);
    }

    public boolean isOnlyConsole() {
        return this.onlyConsole;
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

    public int getExactArgs() {
        return this.exactArgs;
    }

    public int getMaxArgs() {
        return this.maxArgs;
    }

    public QuestionAnswerType<?>[] getRequiredArguments() {
        return this.requiredArguments;
    }

}
