package de.dytanic.cloudnet.console.animation.progressbar;

public class ConsoleDownloadProgressBarAnimation extends ConsoleProgressBarAnimation {

  public ConsoleDownloadProgressBarAnimation(long fullLength, int startValue, char progressChar,
    char lastProgressChar, char emptyChar, String prefix, String suffix) {
    super(fullLength, startValue, progressChar, lastProgressChar, emptyChar, prefix, suffix);
  }

  @Override
  protected String formatValue(long value) {
    return String.format("%.3f", (double) value / 1024D / 1024D); // format to MB
  }
}
