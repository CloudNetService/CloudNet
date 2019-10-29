package de.dytanic.cloudnet.console.animation;

import de.dytanic.cloudnet.console.IConsole;
import lombok.EqualsAndHashCode;
import lombok.ToString;
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
@ToString
@EqualsAndHashCode
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
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        execute(console);
    }

    protected void execute(IConsole console) {
        int percent = (int) ((progressValue * 100) / targetGoal);
        StringBuilder stringBuilder = new StringBuilder();

        if (expand) {
            for (int i = 0; i < percent; i++) {
                if (i % 2 == 0) {
                    stringBuilder.append(progressChar);
                }
            }
        } else {
            for (int i = 0; i < 100; i++) {
                if (i % 2 == 0) {
                    stringBuilder.append(i < percent ? progressChar : " ");
                }
            }
        }

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
                .replace("%percent%", String.valueOf(percent))
                .replace("%time%", new SimpleDateFormat("mm:ss").format(System.currentTimeMillis() - barStart))
                .replace("%target%", String.valueOf(this.targetGoal))
                .replace("%value%", String.valueOf(this.progressValue));
    }

    public long getUpdateInterval() {
        return this.updateInterval;
    }

    public void setUpdateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public long getTargetGoal() {
        return this.targetGoal;
    }

    public void setTargetGoal(long targetGoal) {
        this.targetGoal = targetGoal;
    }

    public long getBarStart() {
        return this.barStart;
    }

    public void setBarStart(long barStart) {
        this.barStart = barStart;
    }

    public boolean isExpand() {
        return this.expand;
    }

    public void setExpand(boolean expand) {
        this.expand = expand;
    }

    public long getProgressValue() {
        return this.progressValue;
    }

    public void setProgressValue(long progressValue) {
        this.progressValue = progressValue;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public char getProgressChar() {
        return this.progressChar;
    }

    public void setProgressChar(char progressChar) {
        this.progressChar = progressChar;
    }

}