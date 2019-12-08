package de.dytanic.cloudnet.console.animation;

public class ConsoleDownloadProgressBarAnimation extends ConsoleProgressBarAnimation {
    public ConsoleDownloadProgressBarAnimation(long fullLength, int startValue, char progressChar, char lastProgressChar, String prefix, String suffix) {
        super(fullLength, startValue, progressChar, lastProgressChar, prefix, suffix);
    }

    @Override
    protected String formatValue(long value) {
        return String.format("%.3f", (double) value / 1024D / 1024D); //format to MB
    }
}
