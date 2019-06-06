package de.dytanic.cloudnet.console.animation;

import de.dytanic.cloudnet.console.IConsole;
import org.fusesource.jansi.Ansi;

import java.text.SimpleDateFormat;

/**
 * The ProgressBar represents a progressing situation of a program part.
 * For example
 * <p>
 * CAProgressBar progressBar = new CAProgressBar(
 * '█',
 * "[%percent%%] ",
 * " | %value%/%target%MB Downloaded... (%time%)",
 * 3,
 * 2048,
 * false
 * );
 * <p>
 * TaskScheduler.runtimeScheduler().schedule(new Runnable() {
 *
 * @Override public void run()
 * {
 * if (progressBar.getProgressValue() < progressBar.getTargetGoal())
 * progressBar.setProgressValue(progressBar.getProgressValue() + 1);
 * }
 * }, 5, -1);
 * consoleProvider.invokeConsoleAnimation(progressBar);
 */
public class CAProgressBar {

    protected long updateInterval, targetGoal, barStart;

    protected boolean expand;

    protected volatile long progressValue;

    protected String prefix, suffix;

    protected char progressChar;

    public CAProgressBar(char progressChar, String prefix, String suffix, long updateInterval, long targetGoal, boolean doExpand) {
        this.progressChar = progressChar;
        this.prefix = prefix;
        this.suffix = suffix;
        this.updateInterval = updateInterval;
        this.targetGoal = targetGoal;
        this.expand = doExpand;
    }

    public void start(IConsole console) {
        execute(console);

        this.barStart = System.currentTimeMillis();
        while (progressValue < targetGoal) {
            execute(console);
            try {
                Thread.sleep(updateInterval);
            } catch (InterruptedException ignored) {
            }
        }

        execute(console);
    }

    protected void execute(IConsole console) {
        int percent = (int) ((progressValue * 100) / targetGoal);
        StringBuilder stringBuilder = new StringBuilder();

        if (expand) {
            for (int i = 0; i < percent; i++)
                if (i % 2 == 0) stringBuilder.append(progressChar);
        } else
            for (int i = 0; i < 100; i++)
                if (i % 2 == 0) stringBuilder.append(i < percent ? progressChar : " ");

        console.write(
                Ansi
                        .ansi()
                        .saveCursorPosition()
                        .cursorUp(1)
                        .eraseLine(Ansi.Erase.ALL)
                        .a(insertPatterns(prefix, percent))
                        .a(stringBuilder.toString())
                        .a(insertPatterns(suffix, percent))
                        .restoreCursorPosition()
                        .toString()
        );
    }

    protected String insertPatterns(String value, int percent) {
        return value
                .replace("%percent%", percent + "")
                .replace("%time%", new SimpleDateFormat("mm:ss").format(System.currentTimeMillis() - barStart))
                .replace("%target%", targetGoal + "")
                .replace("%value%", this.progressValue + "");
    }

    public long getUpdateInterval() {
        return this.updateInterval;
    }

    public long getTargetGoal() {
        return this.targetGoal;
    }

    public long getBarStart() {
        return this.barStart;
    }

    public boolean isExpand() {
        return this.expand;
    }

    public long getProgressValue() {
        return this.progressValue;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public char getProgressChar() {
        return this.progressChar;
    }

    public void setUpdateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public void setTargetGoal(long targetGoal) {
        this.targetGoal = targetGoal;
    }

    public void setBarStart(long barStart) {
        this.barStart = barStart;
    }

    public void setExpand(boolean expand) {
        this.expand = expand;
    }

    public void setProgressValue(long progressValue) {
        this.progressValue = progressValue;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void setProgressChar(char progressChar) {
        this.progressChar = progressChar;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CAProgressBar)) return false;
        final CAProgressBar other = (CAProgressBar) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getUpdateInterval() != other.getUpdateInterval()) return false;
        if (this.getTargetGoal() != other.getTargetGoal()) return false;
        if (this.getBarStart() != other.getBarStart()) return false;
        if (this.isExpand() != other.isExpand()) return false;
        if (this.getProgressValue() != other.getProgressValue()) return false;
        final Object this$prefix = this.getPrefix();
        final Object other$prefix = other.getPrefix();
        if (this$prefix == null ? other$prefix != null : !this$prefix.equals(other$prefix)) return false;
        final Object this$suffix = this.getSuffix();
        final Object other$suffix = other.getSuffix();
        if (this$suffix == null ? other$suffix != null : !this$suffix.equals(other$suffix)) return false;
        if (this.getProgressChar() != other.getProgressChar()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CAProgressBar;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $updateInterval = this.getUpdateInterval();
        result = result * PRIME + (int) ($updateInterval >>> 32 ^ $updateInterval);
        final long $targetGoal = this.getTargetGoal();
        result = result * PRIME + (int) ($targetGoal >>> 32 ^ $targetGoal);
        final long $barStart = this.getBarStart();
        result = result * PRIME + (int) ($barStart >>> 32 ^ $barStart);
        result = result * PRIME + (this.isExpand() ? 79 : 97);
        final long $progressValue = this.getProgressValue();
        result = result * PRIME + (int) ($progressValue >>> 32 ^ $progressValue);
        final Object $prefix = this.getPrefix();
        result = result * PRIME + ($prefix == null ? 43 : $prefix.hashCode());
        final Object $suffix = this.getSuffix();
        result = result * PRIME + ($suffix == null ? 43 : $suffix.hashCode());
        result = result * PRIME + this.getProgressChar();
        return result;
    }

    public String toString() {
        return "CAProgressBar(updateInterval=" + this.getUpdateInterval() + ", targetGoal=" + this.getTargetGoal() + ", barStart=" + this.getBarStart() + ", expand=" + this.isExpand() + ", progressValue=" + this.getProgressValue() + ", prefix=" + this.getPrefix() + ", suffix=" + this.getSuffix() + ", progressChar=" + this.getProgressChar() + ")";
    }
}