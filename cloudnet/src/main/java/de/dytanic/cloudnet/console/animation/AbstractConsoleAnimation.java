package de.dytanic.cloudnet.console.animation;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.console.IConsole;
import jline.console.ConsoleReader;
import lombok.Getter;
import org.fusesource.jansi.Ansi;

import java.util.function.Consumer;

public abstract class AbstractConsoleAnimation implements Runnable {

    private IConsole console;
    private int updateInterval = 25;
    private long startTime;
    private int cursorUp = 1;

    public long getTimeElapsed() {
        return System.currentTimeMillis() - this.startTime;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public void setConsole(IConsole console) {
        if (this.console != null) {
            throw new IllegalStateException("Cannot set console twice");
        }
        this.console = console;
    }

    public IConsole getConsole() {
        return this.console;
    }

    public void addToCursor(int cursor) {
        this.cursorUp += cursor;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public int getUpdateInterval() {
        return this.updateInterval;
    }

    protected void print(String... input) {
        if (input.length == 0)
            return;
        input[0] = "&e" + input[0];
        Ansi ansi = Ansi
                .ansi()
                .saveCursorPosition()
                .cursorUp(this.cursorUp)
                .eraseLine(Ansi.Erase.ALL);
        for (String a : input) {
            ansi.a(a);
        }
        this.console.write(ansi.restoreCursorPosition().toString());
    }

    protected abstract boolean handleTick(); //returns true if the animation is finished and should be cancelled

    @Override
    public final void run() {
        this.startTime = System.currentTimeMillis();
        while (!Thread.interrupted() && !handleTick()) {
            try {
                Thread.sleep(this.updateInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
